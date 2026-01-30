package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.AttemptEntity;
import com.knowgauge.core.model.Attempt;

@Mapper(componentModel = "spring")
public interface AttemptMapper {
    Attempt toDomain(AttemptEntity entity);
    AttemptEntity toEntity(Attempt domain);
}