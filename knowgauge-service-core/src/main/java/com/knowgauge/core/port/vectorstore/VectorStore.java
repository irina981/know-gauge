package com.knowgauge.core.port.vectorstore;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocChunkCount;

public interface VectorStore {

	ChunkEmbedding save(ChunkEmbedding embedding);

	List<ChunkEmbedding> saveAll(List<ChunkEmbedding> embedding);

	Optional<ChunkEmbedding> findByTenantIdAndChunkIdAndEmbeddingModel(Long tenantId, Long chunkId,
			String embeddingModel);

	List<ChunkEmbedding> findByTenantIdAndChunkIdInAndEmbeddingModel(Long tenantId, Collection<Long> chunkIds,
			String embeddingModel);

	long deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(Long tenantId, Long documentId,
			Integer documentVersion, String embeddingModel);

	long deleteByTenantIdAndChunkId(Long tenantId, Long chunkId);

	List<DocChunkCount> loadDocChunkCounts(Collection<Long> tenantIds, String embeddingModel, Collection<Long> documentIds);

	public List<ChunkEmbedding> findCandidates(Collection<Long> tenantIds, Collection<Long> documentIds, int maxChunksPerDoc,
			String embeddingModel, boolean avoidRepeats);

	/**
	 * Similarity search for FOCUSED coverage: the chunks of the given documents
	 * closest to the query embedding (cosine distance), nearest first.
	 *
	 * @param queryEmbedding embedding of the focus text, made with {@code embeddingModel}
	 * @param limit          most rows returned
	 */
	List<ChunkEmbedding> findNearest(Collection<Long> tenantIds, Collection<Long> documentIds, float[] queryEmbedding,
			String embeddingModel, int limit);
}
