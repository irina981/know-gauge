package com.knowgauge.core.port.vectorstore;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.ChunkEmbedding;

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
}
