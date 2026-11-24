package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A workspace an ADMIN creates (ADR015): a personal one with exactly one invitee, an organization with at least one, a shared library with none")
public record TenantCreateInput(
		@Schema(description = "PERSONAL, ORGANIZATION or LIBRARY", example = "ORGANIZATION") @NotBlank String type,
		@Schema(description = "Shown in the app", example = "Acme Training") @NotBlank @Size(max = 200) String name,
		@Schema(description = "Email addresses to invite; each becomes a TENANT_ADMIN of the workspace on accepting", example = "[\"ana@example.com\"]") List<String> emails,
		@Schema(description = "Optional note added to the invitation email") @Size(max = 2000) String message) {
}
