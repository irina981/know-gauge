package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.DocumentChunkEntity;
import com.knowgauge.core.model.DocumentChunk;

@Mapper(componentModel = "spring")
public interface DocumentChunkEntityMapper {
    DocumentChunk toDomain(DocumentChunkEntity entity);
    DocumentChunkEntity toEntity(DocumentChunk domain);
}