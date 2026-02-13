package com.knowgauge.pgvector.repo.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.pgvector.repo.jpa.entity.ChunkEmbeddingEntity;

@Mapper(componentModel = "spring")
public interface ChunkEmbeddingEntityMapper {
    ChunkEmbedding toDomain(ChunkEmbeddingEntity entity);
    ChunkEmbeddingEntity toEntity(ChunkEmbedding domain);
}