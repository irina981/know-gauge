package com.knowgauge.infra.repository.jpa.mapper;

import org.mapstruct.Mapper;

import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.infra.repository.jpa.entity.WorkspaceInvitationEntity;

@Mapper(componentModel = "spring")
public interface WorkspaceInvitationEntityMapper {
	WorkspaceInvitation toDomain(WorkspaceInvitationEntity entity);

	WorkspaceInvitationEntity toEntity(WorkspaceInvitation domain);
}
