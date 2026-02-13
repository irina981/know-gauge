package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.infra.repository.jpa.entity.AttemptAnswerEntity;

@Mapper(componentModel = "spring")
public interface AttemptAnswerEntityMapper {

    @Mapping(target = "id", source = "id")
    AttemptAnswer toDomain(AttemptAnswerEntity entity);

    @Mapping(target = "id", source = "id")
    AttemptAnswerEntity toEntity(AttemptAnswer domain);
}