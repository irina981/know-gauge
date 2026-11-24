package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import com.knowgauge.contract.dto.WorkspaceInvitationDto;
import com.knowgauge.contract.dto.WorkspaceInvitationPreviewDto;
import com.knowgauge.contract.dto.WorkspaceJoinedDto;
import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.workspace.WorkspaceMembershipService.InvitationPreview;
import com.knowgauge.core.service.workspace.WorkspaceMembershipService.Joined;

/** Workspace invitations (ADR015). */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class WorkspaceMapper {

	@Autowired
	protected InvitationProperties invitationProperties;

	@Mapping(target = "link", expression = "java(invitationProperties.workspaceLinkFor(invitation.getPublicToken()))")
	public abstract WorkspaceInvitationDto toDto(WorkspaceInvitation invitation);

	public abstract WorkspaceInvitationPreviewDto toDto(InvitationPreview preview);

	public abstract WorkspaceJoinedDto toDto(Joined joined);
}
