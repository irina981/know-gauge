package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.Document;
import com.knowgauge.infra.repository.jpa.entity.DocumentEntity;

@Mapper(componentModel = "spring")
public interface DocumentEntityMapper {
    Document toDomain(DocumentEntity entity);
    DocumentEntity toEntity(Document domain);
}