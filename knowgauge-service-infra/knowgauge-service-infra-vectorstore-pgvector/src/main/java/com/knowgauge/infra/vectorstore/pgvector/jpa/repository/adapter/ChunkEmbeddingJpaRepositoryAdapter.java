package com.knowgauge.infra.vectorstore.pgvector.jpa.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

@Repository
public class ChunkEmbeddingJpaRepositoryAdapter implements VectorStore {
	@PersistenceContext(unitName = "pgvector")
	private EntityManager em;

	private final ChunkEmbeddingJpaRepository jpaRepository;
	private final ChunkEmbeddingEntityMapper mapper;
	private final EmbeddingService embeddingService;

	public ChunkEmbeddingJpaRepositoryAdapter(ChunkEmbeddingJpaRepository jpaRepository,
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
		long deletedRows = jpaRepository.deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(tenantId,
				documentId, documentVersion, embeddingModel);
		return deletedRows;
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
	public List<ChunkEmbedding> findTop(Long tenantId, Collection<Long> topicIds, Collection<Long> documentIds,
			int limit, TestCoverageMode coverageMode, boolean avoidRepeats) {

		// =========================
		// Step 0: Validate inputs
		// =========================
		// - tenantId: mandatory (multi-tenant isolation)
		// - limit: how many chunk candidates we want to return at most
		if (tenantId == null) {
			throw new IllegalArgumentException("tenantId must not be null");
		}
		if (limit <= 0) {
			return List.of();
		}

		// =========================
		// Step 1: Validate supported coverage modes
		// =========================
		// FOCUSED will later use vector similarity based on query text:
		// queryText -> queryEmbedding -> ORDER BY distance(embedding, queryEmbedding)
		// Until then, we fail fast to avoid silently returning "balanced" data.
		if (coverageMode == TestCoverageMode.FOCUSED) {
			throw new UnsupportedOperationException(
					"FOCUSED coverage mode requires vector similarity search with a query text/embedding. Not implemented yet.");
		}

		// =========================
		// Step 2: Determine embedding model used in the vector store
		// =========================
		// IMPORTANT:
		// - generationModel (LLM) != embeddingModel (vector space)
		// - embeddings are only comparable inside the same embedding model space
		String embeddingModel = embeddingService.modelName();

		// =========================
		// Step 3: Determine active filters (topics/docs)
		// =========================
		// If filters are empty, we treat them as "not applied".
		boolean filterTopics = topicIds != null && !topicIds.isEmpty();
		boolean filterDocs = documentIds != null && !documentIds.isEmpty();

		// =========================
		// Step 4: Discover corpus shape (how many chunks exist per document in scope)
		// =========================
		// We need this because "balanced" selection depends on how many documents and
		// how large they are.
		// Example: 1 huge document + many small docs -> per-doc balancing may
		// underrepresent the huge doc,
		// while per-chunk balancing keeps proportional representation.
		List<DocChunkCount> docCounts = loadDocChunkCounts(tenantId, embeddingModel, filterTopics, topicIds, filterDocs,
				documentIds);
		if (docCounts.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 5: Compute per-document selection limits (allocation)
		// =========================
		// maxChunksPerDocumentMap tells us how many chunks each document is allowed to
		// contribute:
		// documentId -> maxChunksFromThatDocument
		//
		// Two supported strategies:
		// a) BALANCED_PER_DOCS:
		// Each document gets approximately the same chunk budget (diversity across
		// docs).
		// b) BALANCED_PER_DOC_CHUNKS:
		// Each document budget is proportional to its number of available chunks in
		// scope
		// (size-aware balancing; avoids penalizing large docs).
		Map<Long, Integer> maxChunksPerDocumentMap = calculateMaxChunksPerDocument(docCounts, limit, coverageMode);

		// Max quota among all documents; we use it to "over-fetch" candidates per doc
		// once,
		// then apply the exact per-doc budgets in memory.
		int maxChunks = maxChunksPerDocumentMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);

		if (maxChunks <= 0) {
			return List.of();
		}

		// =========================
		// Step 6: Fetch candidate embeddings from DB (balanced pre-selection)
		// =========================
		// We fetch up to 'maxChunks' embeddings per document using a window function:
		// row_number() OVER (PARTITION BY document_id ORDER BY random()) as rn
		//
		// Then we take rn <= maxChunks
		//
		// This gives us an initial candidate set that:
		// - contains a reasonable sample from each document
		// - is already randomized per-document
		//
		// After that, we enforce the EXACT budgets (per document) in Java.
		String sql = buildRankedCandidatesSql(filterTopics, filterDocs);

		@SuppressWarnings("unchecked")
		org.hibernate.query.NativeQuery<ChunkEmbeddingEntity> q = em.createNativeQuery(sql, ChunkEmbeddingEntity.class)
				.setParameter("tenantId", tenantId).setParameter("embeddingModel", embeddingModel)
				.setParameter("maxChunks", maxChunks) // IMPORTANT: must match the SQL parameter name
				.unwrap(org.hibernate.query.NativeQuery.class);

		if (filterTopics) {
			q.setParameterList("topicIds", topicIds);
		}
		if (filterDocs) {
			q.setParameterList("documentIds", documentIds);
		}

		List<ChunkEmbeddingEntity> candidates = q.getResultList();
		if (candidates.isEmpty()) {
			return List.of();
		}

		// =========================
		// Step 7: Final selection in memory (apply per-doc budgets + dedupe)
		// =========================
		// We now build the final result list:
		// - enforce each document's max chunk budget: maxChunksPerDocumentMap
		// - eliminate duplicate content using chunk_checksum (same content repeated
		// across docs/sections)
		// - stop once we reach 'limit'
		//
		// Note:
		// - candidates are randomized per-doc already, but we shuffle to mix documents
		// together
		// (prevents selecting all chunks from one doc early in iteration).
		java.util.Map<Long, Integer> usedPerDoc = new java.util.HashMap<>();
		java.util.Set<String> seenChecksums = new java.util.HashSet<>();
		java.util.List<ChunkEmbedding> result = new java.util.ArrayList<>(limit);

		java.util.Collections.shuffle(candidates);

		for (ChunkEmbeddingEntity e : candidates) {

			// Stop once we reached requested size
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

			// Dedupe: if another chunk with the same checksum was already selected, skip it
			// (prevents identical content from dominating the selection).
			String checksum = e.getChunkChecksum();
			if (checksum != null && !seenChecksums.add(checksum)) {
				continue;
			}

			// TODO (future):
			// avoidRepeats should exclude chunks already used in previous tests (or earlier
			// runs),
			// but we need an "excludedChunkIds" parameter or a retrieval context to
			// implement this.
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
	 * This is used to calculate document budgets (balanced selection).
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
	 * Step 4 helper: For each document in scope, returns how many chunk embeddings
	 * exist (after filters): tenant + embeddingModel + (topicIds?) + (documentIds?)
	 *
	 * This gives us the corpus shape needed for proportional allocation.
	 */
	private List<DocChunkCount> loadDocChunkCounts(Long tenantId, String embeddingModel, boolean filterTopics,
			Collection<Long> topicIds, boolean filterDocs, Collection<Long> documentIds) {

		String sql = """
				SELECT ce.document_id, count(*) AS cnt
				FROM chunk_embeddings ce
				WHERE ce.tenant_id = :tenantId
				  AND ce.embedding_model = :embeddingModel
				  %s
				  %s
				GROUP BY ce.document_id
				""".formatted(filterTopics ? "AND ce.topic_id IN (:topicIds)" : "",
				filterDocs ? "AND ce.document_id IN (:documentIds)" : "");

		org.hibernate.query.NativeQuery<?> q = em.createNativeQuery(sql).setParameter("tenantId", tenantId)
				.setParameter("embeddingModel", embeddingModel).unwrap(org.hibernate.query.NativeQuery.class);

		if (filterTopics) {
			q.setParameterList("topicIds", topicIds);
		}
		if (filterDocs) {
			q.setParameterList("documentIds", documentIds);
		}

		@SuppressWarnings("unchecked")
		List<Object[]> rows = (List<Object[]>) q.getResultList();

		List<DocChunkCount> out = new java.util.ArrayList<>(rows.size());
		for (Object[] r : rows) {
			Long docId = ((Number) r[0]).longValue();
			long cnt = ((Number) r[1]).longValue();
			out.add(new DocChunkCount(docId, cnt));
		}
		return out;
	}

	/**
	 * Step 5 helper: Calculates how many chunks EACH document is allowed to
	 * contribute to the final result.
	 *
	 * Output: documentId -> maxChunksFromThatDocument
	 *
	 * Supported strategies: - BALANCED_PER_DOCS: All documents are treated equally:
	 * each gets roughly ceil(limit / docCount). This maximizes document diversity.
	 *
	 * - BALANCED_PER_DOC_CHUNKS: Documents get chunk budgets proportional to their
	 * available chunks in scope. This handles highly uneven document sizes better.
	 *
	 * Implementation details for BALANCED_PER_DOC_CHUNKS: 1) Initial proportional
	 * allocation using round(share * limit). 2) Fix rounding drift so total
	 * allocation equals 'limit' (or as close as possible): - if under-allocated:
	 * add 1 to largest documents first - if over-allocated: remove 1 from documents
	 * with largest allocated budgets first
	 */
	private Map<Long, Integer> calculateMaxChunksPerDocument(List<DocChunkCount> docCounts, int limit,
			TestCoverageMode mode) {

		Map<Long, Integer> maxChunksPerDocumentMap = new java.util.HashMap<>();

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
	 * Step 6 helper: Fetch candidate embeddings using a window function to cap "how
	 * many rows per document" we fetch.
	 *
	 * Why: - We want to avoid pulling the entire embedding table for large
	 * documents. - We only need a sample per doc to later apply budgets and dedupe.
	 *
	 * Mechanism: - row_number() partitions by document_id and randomizes within
	 * each doc - rn <= :maxChunks keeps at most :maxChunks candidates per doc
	 *
	 * IMPORTANT: - Parameter name must match what you set in code. - In your
	 * current code you set :maxChunks, so SQL must use :maxChunks (NOT :maxQuota).
	 */
	private String buildRankedCandidatesSql(boolean filterTopics, boolean filterDocs) {
		return """
				WITH ranked AS (
				    SELECT ce.*,
				           row_number() OVER (PARTITION BY ce.document_id ORDER BY random()) AS rn
				    FROM chunk_embeddings ce
				    WHERE ce.tenant_id = :tenantId
				      AND ce.embedding_model = :embeddingModel
				      %s
				      %s
				)
				SELECT *
				FROM ranked
				WHERE rn <= :maxChunks
				""".formatted(filterTopics ? "AND ce.topic_id IN (:topicIds)" : "",
				filterDocs ? "AND ce.document_id IN (:documentIds)" : "");
	}

}
