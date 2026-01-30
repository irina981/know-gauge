package com.knowgauge.core.port.repository;

import java.util.Optional;

import com.knowgauge.core.model.ChunkEmbedding;

public interface ChunkEmbeddingRepository {
	ChunkEmbedding save(ChunkEmbedding embedding);

	Optional<ChunkEmbedding> findByChunkId(Long chunkId);
}