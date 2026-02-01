package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.port.repository.ChunkEmbeddingRepository;
import com.knowgauge.repo.jpa.mapper.ChunkEmbeddingEntityMapper;
import com.knowgauge.repo.jpa.repository.ChunkEmbeddingJpaRepository;

@Repository
@Transactional
public class ChunkEmbeddingJpaRepositoryAdapter implements ChunkEmbeddingRepository {

    private final ChunkEmbeddingJpaRepository jpaRepository;
    private final ChunkEmbeddingEntityMapper mapper;

    public ChunkEmbeddingJpaRepositoryAdapter(ChunkEmbeddingJpaRepository jpaRepository, ChunkEmbeddingEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ChunkEmbedding save(ChunkEmbedding domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChunkEmbedding> findByChunkId(Long chunkId) {
        return jpaRepository.findByChunkId(chunkId).map(mapper::toDomain);
    }
}
