package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A new workspace and the invitations sent for it")
public record TenantCreatedDto(
		TenantSummaryDto tenant,
		@Schema(description = "One per invitee; a failed send has its error") List<TenantInvitationDto> invitations) {
}
