package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.infra.repository.jpa.entity.GenerationRunEntity;

@Mapper(componentModel = "spring")
public interface GenerationRunEntityMapper {
    GenerationRun toDomain(GenerationRunEntity entity);
    GenerationRunEntity toEntity(GenerationRun domain);
}