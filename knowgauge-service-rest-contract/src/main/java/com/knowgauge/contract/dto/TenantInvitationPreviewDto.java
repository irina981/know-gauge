package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a workspace invitation link leads to (public, no ids)")
public record TenantInvitationPreviewDto(
		@Schema(description = "Name of the workspace", example = "Acme Training") String tenantName,
		@Schema(description = "PERSONAL or ORGANIZATION", example = "ORGANIZATION") String tenantType,
		@Schema(description = "The invited address", example = "ana@example.com") String email,
		@Schema(description = "Whether it has been accepted") boolean accepted) {
}
