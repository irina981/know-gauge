package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "People to invite to a workspace (ADR015); an address invited before gets its link again")
public record TenantInviteInput(
		@Schema(description = "Email addresses; duplicates are merged", example = "[\"ana@example.com\"]") @NotEmpty List<String> emails,
		@Schema(description = "Optional note added to the email") @Size(max = 2000) String message,
		@Schema(description = "Role the invitees get on accepting (ADR016): TENANT_ADMIN (default), TENANT_USER or TENANT_VIEWER. A personal workspace takes only TENANT_ADMIN", example = "TENANT_USER") String role) {
}
