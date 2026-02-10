package com.knowgauge.core.port.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.ChunkEmbedding;

public interface ChunkEmbeddingRepository {

	ChunkEmbedding save(ChunkEmbedding embedding);

	Optional<ChunkEmbedding> findByTenantIdAndChunkIdAndEmbeddingModel(Long tenantId, Long chunkId,
			String embeddingModel);

	List<ChunkEmbedding> findByTenantIdAndChunkIdInAndEmbeddingModel(Long tenantId, Collection<Long> chunkIds,
			String embeddingModel);

	void deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(Long tenantId, Long documentId,
			Integer documentVersion, String embeddingModel);

	void deleteByTenantIdAndChunkId(Long tenantId, Long chunkId);
}
