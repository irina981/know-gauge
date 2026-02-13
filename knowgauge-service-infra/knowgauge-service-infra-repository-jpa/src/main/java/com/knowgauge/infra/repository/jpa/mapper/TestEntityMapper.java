package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.Test;
import com.knowgauge.infra.repository.jpa.entity.TestEntity;

@Mapper(componentModel = "spring")
public interface TestEntityMapper {
    Test toDomain(TestEntity entity);
    TestEntity toEntity(Test domain);
}