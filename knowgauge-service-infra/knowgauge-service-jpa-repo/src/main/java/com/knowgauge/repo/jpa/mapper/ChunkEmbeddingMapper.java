package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.ChunkEmbeddingEntity;
import com.knowgauge.core.model.ChunkEmbedding;

@Mapper(componentModel = "spring")
public interface ChunkEmbeddingMapper {
    ChunkEmbedding toDomain(ChunkEmbeddingEntity entity);
    ChunkEmbeddingEntity toEntity(ChunkEmbedding domain);
}