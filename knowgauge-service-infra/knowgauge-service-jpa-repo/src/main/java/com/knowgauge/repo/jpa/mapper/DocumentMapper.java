package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.DocumentEntity;
import com.knowgauge.core.model.Document;

@Mapper(componentModel = "spring")
public interface DocumentMapper {
    Document toDomain(DocumentEntity entity);
    DocumentEntity toEntity(Document domain);
}