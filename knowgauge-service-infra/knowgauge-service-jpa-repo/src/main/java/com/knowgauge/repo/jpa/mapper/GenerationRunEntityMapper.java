package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.GenerationRunEntity;
import com.knowgauge.core.model.GenerationRun;

@Mapper(componentModel = "spring")
public interface GenerationRunEntityMapper {
    GenerationRun toDomain(GenerationRunEntity entity);
    GenerationRunEntity toEntity(GenerationRun domain);
}