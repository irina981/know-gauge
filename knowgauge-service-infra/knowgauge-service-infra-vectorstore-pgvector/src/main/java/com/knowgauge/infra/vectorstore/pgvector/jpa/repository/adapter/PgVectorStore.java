package com.knowgauge.infra.vectorstore.pgvector.jpa.repository.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.port.embedding.EmbeddingService;
import com.knowgauge.core.port.vectorstore.VectorStore;
import com.knowgauge.infra.vectorstore.pgvector.jpa.entity.ChunkEmbeddingEntity;
import com.knowgauge.infra.vectorstore.pgvector.jpa.mapper.ChunkEmbeddingEntityMapper;
import com.knowgauge.infra.vectorstore.pgvector.jpa.repository.ChunkEmbeddingJpaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * PgVector-backed VectorStore adapter that retrieves chunk embeddings from the
 * pgvector database.
 *
 * This adapter currently supports "balanced random selection" strategies: -
 * BALANCED_PER_DOCS: diversify across documents (each document contributes ~
 * equally) - BALANCED_PER_DOC_CHUNKS: proportional to document chunk volume
 * (size-aware balancing)
 *
 * NOTE: This is NOT a semantic similarity retrieval yet. - FOCUSED mode would
 * require query embedding + ORDER BY distance(embedding, queryEmbedding).
 *
 * Selection approach overview (BALANCED modes): 1) Compute corpus shape (how
 * many chunks exist per document in the scope). 2) Compute per-document quotas
 * (budgets) based on selected coverage mode. 3) Over-fetch candidates: up to
 * maxQuota rows per document using window function row_number(). 4) Shuffle
 * candidates to mix documents. 5) Apply budgets + checksum dedupe in-memory
 * until we reach the requested limit.
 *
 * This keeps DB load bounded even for very large documents while still
 * returning a diverse set.
 */
@Repository
public class PgVectorStore implements VectorStore {

	@PersistenceContext(unitName = "pgvector")
	private EntityManager em;

	private final ChunkEmbeddingJpaRepository jpaRepository;
	private final ChunkEmbeddingEntityMapper mapper;
	private final EmbeddingService embeddingService;

	public PgVectorStore(ChunkEmbeddingJpaRepository jpaRepository,
			ChunkEmbeddingEntityMapper mapper, EmbeddingService embeddingService) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
		this.embeddingService = embeddingService;
	}

	@Override
	public ChunkEmbedding save(ChunkEmbedding domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<ChunkEmbedding> findByTenantIdAndChunkIdAndEmbeddingModel(Long tenantId, Long chunkId,
			String embeddingModel) {
		return jpaRepository.findByTenantIdAndChunkIdAndEmbeddingModel(tenantId, chunkId, embeddingModel)
				.map(mapper::toDomain);
	}

	@Override
	public List<ChunkEmbedding> findByTenantIdAndChunkIdInAndEmbeddingModel(Long tenantId, Collection<Long> chunkIds,
			String embeddingModel) {
		return jpaRepository.findByTenantIdAndChunkIdInAndEmbeddingModel(tenantId, chunkIds, embeddingModel).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	public long deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(Long tenantId, Long documentId,
			Integer documentVersion, String embeddingModel) {
		return jpaRepository.deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(tenantId, documentId,
				documentVersion, embeddingModel);
	}

	@Override
	public long deleteByTenantIdAndChunkId(Long tenantId, Long chunkId) {
		return jpaRepository.deleteByTenantIdAndChunkId(tenantId, chunkId);
	}

	@Override
	public List<ChunkEmbedding> saveAll(List<ChunkEmbedding> embeddings) {
		return jpaRepository.saveAll(embeddings.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain)
				.toList();
	}

	@Override
	public List<ChunkEmbedding> findTop(Long tenantId, Collection<Long> documentIds, int limit,
			TestCoverageMode coverageMode, boolean avoidRepeats) {

		// =========================
		// Step 0: Validate inputs
		// =========================
		// tenantId is mandatory for isolation.
		// limit<=0 returns empty.
		// empty documentIds means "no corpus scope" -> return empty (no candidates).
		if (tenantId == null) {
			throw new IllegalArgumentException("tenantId must not be null");
		}
		if (limit <= 0) {
			return List.of();
		}
		if (documentIds == null || documentIds.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 1: Validate supported coverage modes
		// =========================
		// FOCUSED requires query embedding + vector distance ordering.
		// Until implemented, fail-fast to avoid silently returning random/balanced
		// results.
		if (coverageMode == TestCoverageMode.FOCUSED) {
			throw new UnsupportedOperationException(
					"FOCUSED coverage mode requires vector similarity search with a query text/embedding. Not implemented yet.");
		}

		// =========================
		// Step 2: Determine embedding model used in the vector store
		// =========================
		// IMPORTANT:
		// - generationModel (LLM) != embeddingModel (vector space model)
		// - embeddings are comparable only within the same embedding_model
		String embeddingModel = embeddingService.modelName();

		// =========================
		// Step 3: Discover corpus shape (chunks per document in scope)
		// =========================
		// We need doc-level chunk counts because "balanced" selection depends on:
		// - how many documents exist in the scope
		// - how large each document is (chunk volume)
		//
		// Example: one 100-page doc + many 3-page docs:
		// - PER_DOC may under-represent the big doc
		// - PER_DOC_CHUNKS keeps proportional representation
		List<DocChunkCount> docCounts = loadDocChunkCounts(tenantId, embeddingModel, documentIds);
		if (docCounts.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 4: Compute per-document quotas (allocation)
		// =========================
		// For each document: documentId -> maxChunks allowed from that doc.
		//
		// BALANCED_PER_DOCS:
		// - each document gets roughly ceil(limit/docCount)
		//
		// BALANCED_PER_DOC_CHUNKS:
		// - quotas proportional to each document's chunkCount
		// - rounding drift is corrected so total allocated ~= limit
		Map<Long, Integer> maxChunksPerDocumentMap = calculateMaxChunksPerDocument(docCounts, limit, coverageMode);

		// Max quota among all documents; used to over-fetch bounded candidates per doc.
		int maxChunks = maxChunksPerDocumentMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
		if (maxChunks <= 0) {
			return List.of();
		}

		// =========================
		// Step 5: Fetch ranked candidates from DB (bounded per-doc sampling)
		// =========================
		// Over-fetch up to 'maxChunks' candidates per document using:
		// row_number() OVER (PARTITION BY document_id ORDER BY random())
		//
		// This prevents pulling all embeddings for huge documents.
		// Final per-document quotas are applied in memory.
		String sql = buildRankedCandidatesSql();

		@SuppressWarnings("unchecked")
		NativeQuery<ChunkEmbeddingEntity> q = em.createNativeQuery(sql, ChunkEmbeddingEntity.class)
				.setParameter("tenantId", tenantId).setParameter("embeddingModel", embeddingModel)
				.setParameter("maxChunks", maxChunks).unwrap(NativeQuery.class);

		q.setParameterList("documentIds", documentIds);

		List<ChunkEmbeddingEntity> candidates = q.getResultList();
		if (candidates.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 6: Final selection in memory (apply quotas + dedupe + limit)
		// =========================
		// - shuffle candidates to mix documents
		// - enforce per-doc quota
		// - dedupe by chunk_checksum (prevents identical content dominating)
		// - stop at requested limit
		//
		// TODO: avoidRepeats
		// - currently avoidRepeats has no effect
		// - should exclude chunks already used in previous tests / sessions
		// - requires an "excludedChunkIds" or retrieval context input
		Map<Long, Integer> usedPerDoc = new HashMap<>();
		Set<String> seenChecksums = new HashSet<>();
		List<ChunkEmbedding> result = new ArrayList<>(limit);

		// Shuffle to mix docs; prevents taking all candidates from the same doc early
		// due to list ordering.
		Collections.shuffle(candidates);

		for (ChunkEmbeddingEntity e : candidates) {

			if (result.size() >= limit) {
				break;
			}

			Long docId = e.getDocumentId();

			// Document budget (max allowed chunks)
			int maxChunksForDoc = maxChunksPerDocumentMap.getOrDefault(docId, 0);
			if (maxChunksForDoc <= 0) {
				continue;
			}

			// How many have we already taken from this document?
			int used = usedPerDoc.getOrDefault(docId, 0);
			if (used >= maxChunksForDoc) {
				continue;
			}

			// Dedupe: identical content should not dominate the selection.
			String checksum = e.getChunkChecksum();
			if (checksum != null && !seenChecksums.add(checksum)) {
				continue;
			}

			usedPerDoc.put(docId, used + 1);
			result.add(mapper.toDomain(e));
		}

		return result;
	}

	/**
	 * Holds document-level corpus stats after applying the active filters: -
	 * documentId: which document the chunk belongs to - chunkCount: how many
	 * embedding rows are available for that document in the scope
	 *
	 * Used to calculate per-document quotas (balanced selection).
	 */
	private static final class DocChunkCount {
		final Long documentId;
		final long chunkCount;

		DocChunkCount(Long documentId, long chunkCount) {
			this.documentId = documentId;
			this.chunkCount = chunkCount;
		}
	}

	/**
	 * Returns chunk counts per document within the active scope.
	 *
	 * Scope filters: - tenant_id - embedding_model - document_id IN (:documentIds)
	 *
	 * This is used to calculate per-document quotas (balanced selection).
	 */
	private List<DocChunkCount> loadDocChunkCounts(Long tenantId, String embeddingModel, Collection<Long> documentIds) {

		String sql = """
				SELECT ce.document_id, count(*) AS cnt
				FROM chunk_embeddings ce
				WHERE ce.tenant_id = :tenantId
				  AND ce.embedding_model = :embeddingModel
				  AND ce.document_id IN (:documentIds)
				GROUP BY ce.document_id
				""";

		NativeQuery<?> q = em.createNativeQuery(sql).setParameter("tenantId", tenantId)
				.setParameter("embeddingModel", embeddingModel).unwrap(NativeQuery.class);

		q.setParameterList("documentIds", documentIds);

		@SuppressWarnings("unchecked")
		List<Object[]> rows = (List<Object[]>) q.getResultList();

		List<DocChunkCount> out = new ArrayList<>(rows.size());
		for (Object[] r : rows) {
			Long docId = ((Number) r[0]).longValue();
			long cnt = ((Number) r[1]).longValue();
			out.add(new DocChunkCount(docId, cnt));
		}
		return out;
	}

	/**
	 * Calculates how many chunks EACH document is allowed to contribute to the
	 * final result.
	 *
	 * Output: documentId -> maxChunksFromThatDocument
	 *
	 * Strategies: - BALANCED_PER_DOCS: Each document gets roughly
	 * ceil(limit/docCount). Maximizes diversity across docs.
	 *
	 * - BALANCED_PER_DOC_CHUNKS: Each document quota is proportional to its
	 * available chunk volume. Handles uneven document sizes better.
	 *
	 * Notes: - Some documents may end up with 0 quota after proportional rounding
	 * (small docs). - Rounding drift is corrected so total allocated ~= limit.
	 */
	private Map<Long, Integer> calculateMaxChunksPerDocument(List<DocChunkCount> docCounts, int limit,
			TestCoverageMode mode) {

		Map<Long, Integer> maxChunksPerDocumentMap = new HashMap<>();

		int docN = docCounts.size();

		if (mode == TestCoverageMode.BALANCED_PER_DOCS) {
			int perDoc = (int) Math.ceil((double) limit / docN);
			perDoc = Math.max(1, Math.min(limit, perDoc));

			for (DocChunkCount dc : docCounts) {
				maxChunksPerDocumentMap.put(dc.documentId, perDoc);
			}
			return maxChunksPerDocumentMap;
		}

		// BALANCED_PER_DOC_CHUNKS (proportional to corpus size)
		long totalChunks = docCounts.stream().mapToLong(dc -> dc.chunkCount).sum();
		if (totalChunks <= 0) {
			return maxChunksPerDocumentMap;
		}

		// 1) Initial proportional allocation
		for (DocChunkCount dc : docCounts) {
			double share = (double) dc.chunkCount / (double) totalChunks;
			int q = (int) Math.round(share * limit);
			maxChunksPerDocumentMap.put(dc.documentId, Math.max(0, q));
		}

		// 2) Fix rounding drift (ensure total allocated ~= limit)
		int allocated = maxChunksPerDocumentMap.values().stream().mapToInt(Integer::intValue).sum();
		int diff = limit - allocated;

		// Under-allocation: distribute remaining slots to biggest documents first
		if (diff > 0) {
			List<DocChunkCount> sortedBySizeDesc = docCounts.stream()
					.sorted((a, b) -> Long.compare(b.chunkCount, a.chunkCount)).toList();

			for (DocChunkCount dc : sortedBySizeDesc) {
				if (diff == 0)
					break;
				maxChunksPerDocumentMap.put(dc.documentId, maxChunksPerDocumentMap.get(dc.documentId) + 1);
				diff--;
			}
		}

		// Over-allocation: remove slots from documents with largest current budgets
		// first
		if (diff < 0) {
			int toRemove = -diff;

			List<DocChunkCount> sortedByBudgetDesc = docCounts.stream()
					.sorted((a, b) -> Integer.compare(maxChunksPerDocumentMap.getOrDefault(b.documentId, 0),
							maxChunksPerDocumentMap.getOrDefault(a.documentId, 0)))
					.toList();

			for (DocChunkCount dc : sortedByBudgetDesc) {
				if (toRemove == 0)
					break;

				int cur = maxChunksPerDocumentMap.getOrDefault(dc.documentId, 0);
				if (cur > 0) {
					maxChunksPerDocumentMap.put(dc.documentId, cur - 1);
					toRemove--;
				}
			}
		}

		return maxChunksPerDocumentMap;
	}

	/**
	 * SQL that returns up to :maxChunks random candidate rows per document, using a
	 * window function.
	 *
	 * Mechanics: - row_number() partitions by document_id and randomizes within
	 * each partition - rn <= :maxChunks caps fetched rows per document
	 *
	 * This keeps DB work bounded and avoids pulling the entire embeddings table.
	 *
	 * IMPORTANT: - parameter names must match code: :tenantId, :embeddingModel,
	 * :documentIds, :maxChunks
	 */
	private String buildRankedCandidatesSql() {
		return """
				WITH ranked AS (
				    SELECT ce.*,
				           row_number() OVER (PARTITION BY ce.document_id ORDER BY random()) AS rn
				    FROM chunk_embeddings ce
				    WHERE ce.tenant_id = :tenantId
				      AND ce.embedding_model = :embeddingModel
				      AND ce.document_id IN (:documentIds)
				)
				SELECT
					ranked.id,
					ranked.tenant_id,
					ranked.topic_id,
					ranked.document_id,
					ranked.document_version,
					ranked.section_id,
					ranked.chunk_id,
					ranked.chunk_checksum,
					ranked.embedding_model,
					ranked.embedding,
					ranked.created_at,
					ranked.created_by,
					ranked.updated_at,
					ranked.updated_by
				FROM ranked
				WHERE ranked.rn <= :maxChunks
				""";
	}
}
