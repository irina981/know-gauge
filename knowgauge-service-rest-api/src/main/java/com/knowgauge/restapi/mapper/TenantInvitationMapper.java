package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import com.knowgauge.contract.dto.TenantInvitationDto;
import com.knowgauge.contract.dto.TenantInvitationPreviewDto;
import com.knowgauge.contract.dto.TenantJoinedDto;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.tenant.TenantMembershipService.InvitationPreview;
import com.knowgauge.core.service.tenant.TenantMembershipService.Joined;

/** Workspace invitations (ADR015). */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class TenantInvitationMapper {

	@Autowired
	protected InvitationProperties invitationProperties;

	@Mapping(target = "link", expression = "java(invitationProperties.tenantLinkFor(invitation.getPublicToken()))")
	public abstract TenantInvitationDto toDto(TenantInvitation invitation);

	public abstract TenantInvitationPreviewDto toDto(InvitationPreview preview);

	public abstract TenantJoinedDto toDto(Joined joined);
}
