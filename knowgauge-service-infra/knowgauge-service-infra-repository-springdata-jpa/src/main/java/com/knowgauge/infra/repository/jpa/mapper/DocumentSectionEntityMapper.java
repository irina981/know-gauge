package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.DocumentSection;
import com.knowgauge.infra.repository.jpa.entity.DocumentSectionEntity;

@Mapper(componentModel = "spring")
public interface DocumentSectionEntityMapper {
    DocumentSection toDomain(DocumentSectionEntity entity);
    DocumentSectionEntity toEntity(DocumentSection domain);
}