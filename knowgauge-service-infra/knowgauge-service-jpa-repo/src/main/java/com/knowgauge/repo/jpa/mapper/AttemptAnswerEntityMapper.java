package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.repo.jpa.entity.AttemptAnswerEntity;

@Mapper(componentModel = "spring")
public interface AttemptAnswerEntityMapper {

    @Mapping(target = "id", source = "id")
    AttemptAnswer toDomain(AttemptAnswerEntity entity);

    @Mapping(target = "id", source = "id")
    AttemptAnswerEntity toEntity(AttemptAnswer domain);
}