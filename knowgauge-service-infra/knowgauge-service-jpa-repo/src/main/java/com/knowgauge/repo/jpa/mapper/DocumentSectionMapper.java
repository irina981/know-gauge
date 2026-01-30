package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.DocumentSection;
import com.knowgauge.repo.jpa.entity.DocumentSectionEntity;

@Mapper(componentModel = "spring")
public interface DocumentSectionMapper {
    DocumentSection toDomain(DocumentSectionEntity entity);
    DocumentSectionEntity toEntity(DocumentSection domain);
}