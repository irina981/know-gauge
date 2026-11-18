package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.infra.repository.jpa.entity.TenantEntity;

@Mapper(componentModel = "spring")
public interface TenantEntityMapper {
	Tenant toDomain(TenantEntity entity);

	TenantEntity toEntity(Tenant domain);
}
