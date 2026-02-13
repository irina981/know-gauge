package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.TestQuestionEntity;
import com.knowgauge.core.model.TestQuestion;

@Mapper(componentModel = "spring")
public interface TestQuestionEntityMapper {
    TestQuestion toDomain(TestQuestionEntity entity);
    TestQuestionEntity toEntity(TestQuestion domain);
}