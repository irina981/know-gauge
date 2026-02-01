package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.TestEntity;
import com.knowgauge.core.model.Test;

@Mapper(componentModel = "spring")
public interface TestEntityMapper {
    Test toDomain(TestEntity entity);
    TestEntity toEntity(Test domain);
}