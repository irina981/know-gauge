package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.TokenPrice;
import com.knowgauge.infra.repository.jpa.entity.TokenPriceEntity;

@Mapper(componentModel = "spring")
public interface TokenPriceEntityMapper {

	TokenPrice toDomain(TokenPriceEntity entity);

	TokenPriceEntity toEntity(TokenPrice domain);
}
