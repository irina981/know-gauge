package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a workspace invitation link leads to (public, no ids)")
public record WorkspaceInvitationPreviewDto(
		@Schema(description = "Name of the workspace", example = "Acme Training") String workspaceName,
		@Schema(description = "PERSONAL or ORGANIZATION", example = "ORGANIZATION") String workspaceType,
		@Schema(description = "The invited address", example = "ana@example.com") String email,
		@Schema(description = "Whether it has been accepted") boolean accepted) {
}
