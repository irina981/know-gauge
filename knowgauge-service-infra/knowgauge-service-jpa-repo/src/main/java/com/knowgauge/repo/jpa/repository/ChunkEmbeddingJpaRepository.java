package com.knowgauge.repo.jpa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.ChunkEmbeddingEntity;

@Repository
public interface ChunkEmbeddingJpaRepository extends JpaRepository<ChunkEmbeddingEntity, Long> {

	Optional<ChunkEmbeddingEntity> findByChunkId(Long chunkId);
}