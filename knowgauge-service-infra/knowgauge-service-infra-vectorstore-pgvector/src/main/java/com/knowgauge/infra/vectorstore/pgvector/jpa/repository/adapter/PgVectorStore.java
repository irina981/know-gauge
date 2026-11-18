package com.knowgauge.infra.vectorstore.pgvector.jpa.repository.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocChunkCount;
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
 * FOCUSED mode is a similarity search instead ({@link #findNearest}): ORDER BY
 * cosine distance to the embedded focus text.
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

	public PgVectorStore(ChunkEmbeddingJpaRepository jpaRepository, ChunkEmbeddingEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
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
	public List<ChunkEmbedding> findCandidates(Collection<Long> tenantIds, Collection<Long> documentIds, int maxChunksPerDoc,
			String embeddingModel, boolean avoidRepeats) {

		String sql = buildRankedCandidatesSql();

		@SuppressWarnings("unchecked")
		NativeQuery<ChunkEmbeddingEntity> q = em.createNativeQuery(sql, ChunkEmbeddingEntity.class)
				.setParameter("embeddingModel", embeddingModel)
				.setParameter("maxChunks", maxChunksPerDoc).unwrap(NativeQuery.class);

		q.setParameterList("documentIds", documentIds);
		q.setParameterList("tenantIds", tenantIds);

		List<ChunkEmbeddingEntity> candidates = q.getResultList();
		if (candidates.isEmpty()) {
			return List.of();
		}

		return candidates.stream().map(mapper::toDomain).toList();
	}

	/**
	 * IVFFlat probes for {@link #findNearest}. Equal to the index's {@code lists}
	 * (100, see the pgvector migration), which makes the search exact. With the
	 * default of 1 probe the index only scans one list and, combined with the
	 * tenant/model/document filter, can return far fewer rows than requested.
	 */
	private static final int EXACT_SEARCH_PROBES = 100;

	@Override
	@Transactional(transactionManager = "vectorTransactionManager", readOnly = true)
	public List<ChunkEmbedding> findNearest(Collection<Long> tenantIds, Collection<Long> documentIds, float[] queryEmbedding,
			String embeddingModel, int limit) {
		// Only for this transaction (is_local = true), so other queries keep their settings.
		em.createNativeQuery("SELECT set_config('ivfflat.probes', :probes, true)")
				.setParameter("probes", String.valueOf(EXACT_SEARCH_PROBES)).getSingleResult();

		String sql = """
				SELECT ce.id, ce.tenant_id, ce.topic_id, ce.document_id, ce.document_version, ce.section_id,
				       ce.chunk_id, ce.chunk_checksum, ce.embedding_model, ce.embedding,
				       ce.created_at, ce.created_by, ce.updated_at, ce.updated_by
				FROM chunk_embeddings ce
				WHERE ce.tenant_id IN (:tenantIds)
				  AND ce.embedding_model = :embeddingModel
				  AND ce.document_id IN (:documentIds)
				ORDER BY ce.embedding <=> CAST(:query AS vector)
				LIMIT :limit
				""";

		@SuppressWarnings("unchecked")
		NativeQuery<ChunkEmbeddingEntity> q = em.createNativeQuery(sql, ChunkEmbeddingEntity.class)
				.setParameter("embeddingModel", embeddingModel)
				.setParameter("query", toVectorLiteral(queryEmbedding)).setParameter("limit", limit)
				.unwrap(NativeQuery.class);
		q.setParameterList("documentIds", documentIds);
		q.setParameterList("tenantIds", tenantIds);

		return q.getResultList().stream().map(mapper::toDomain).toList();
	}

	/** pgvector's text form: {@code [0.1,0.2,...]}. */
	static String toVectorLiteral(float[] vector) {
		StringBuilder sb = new StringBuilder(vector.length * 12).append('[');
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(vector[i]);
		}
		return sb.append(']').toString();
	}

	/**
	 * Returns chunk counts per document within the active scope.
	 *
	 * Scope filters: - tenant_id IN (:tenantIds) - embedding_model - document_id IN (:documentIds)
	 *
	 * This is used to calculate per-document quotas (balanced selection).
	 */
	public List<DocChunkCount> loadDocChunkCounts(Collection<Long> tenantIds, String embeddingModel, Collection<Long> documentIds) {

		String sql = """
				SELECT ce.document_id, count(*) AS cnt
				FROM chunk_embeddings ce
				WHERE ce.tenant_id IN (:tenantIds)
				  AND ce.embedding_model = :embeddingModel
				  AND ce.document_id IN (:documentIds)
				GROUP BY ce.document_id
				""";

		NativeQuery<?> q = em.createNativeQuery(sql)
				.setParameter("embeddingModel", embeddingModel).unwrap(NativeQuery.class);

		q.setParameterList("documentIds", documentIds);
		q.setParameterList("tenantIds", tenantIds);

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
	 * SQL that returns up to :maxChunks random candidate rows per document, using a
	 * window function.
	 *
	 * Mechanics: - row_number() partitions by document_id and randomizes within
	 * each partition - rn <= :maxChunks caps fetched rows per document
	 *
	 * This keeps DB work bounded and avoids pulling the entire embeddings table.
	 *
	 * IMPORTANT: - parameter names must match code: :tenantIds, :embeddingModel,
	 * :documentIds, :maxChunks
	 */
	private String buildRankedCandidatesSql() {
		return """
				WITH ranked AS (
				    SELECT ce.*,
				           row_number() OVER (PARTITION BY ce.document_id ORDER BY random()) AS rn
				    FROM chunk_embeddings ce
				    WHERE ce.tenant_id IN (:tenantIds)
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
