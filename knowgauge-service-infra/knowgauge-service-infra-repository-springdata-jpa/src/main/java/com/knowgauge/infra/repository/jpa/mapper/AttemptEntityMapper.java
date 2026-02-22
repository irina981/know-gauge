package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.Attempt;
import com.knowgauge.infra.repository.jpa.entity.AttemptEntity;

@Mapper(componentModel = "spring")
public interface AttemptEntityMapper {
    Attempt toDomain(AttemptEntity entity);
    AttemptEntity toEntity(Attempt domain);
}