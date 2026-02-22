package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.infra.repository.jpa.entity.DocumentChunkEntity;

@Mapper(componentModel = "spring")
public interface DocumentChunkEntityMapper {
    DocumentChunk toDomain(DocumentChunkEntity entity);
    DocumentChunkEntity toEntity(DocumentChunk domain);
}