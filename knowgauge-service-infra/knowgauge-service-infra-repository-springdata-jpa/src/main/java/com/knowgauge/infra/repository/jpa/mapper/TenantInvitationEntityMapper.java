package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.infra.repository.jpa.entity.TenantInvitationEntity;

@Mapper(componentModel = "spring")
public interface TenantInvitationEntityMapper {
	TenantInvitation toDomain(TenantInvitationEntity entity);

	TenantInvitationEntity toEntity(TenantInvitation domain);
}
