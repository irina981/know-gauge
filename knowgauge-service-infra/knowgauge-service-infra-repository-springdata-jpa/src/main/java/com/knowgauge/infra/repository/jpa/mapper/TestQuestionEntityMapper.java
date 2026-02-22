package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionEntity;

@Mapper(componentModel = "spring")
public interface TestQuestionEntityMapper {
    TestQuestion toDomain(TestQuestionEntity entity);
    TestQuestionEntity toEntity(TestQuestion domain);
}