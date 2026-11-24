package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A user of the current organization workspace, as its tenant admins see them (ADR016)")
public record TenantUserDto(
		Long id,
		@Schema(description = "Email from the identity provider at the last sign-in") String email,
		String displayName,
		@Schema(description = "The user's roles from user_roles, e.g. TENANT_ADMIN (ADR012)") List<String> roles,
		@Schema(description = "When the user first signed in to KnowGauge") Instant createdAt,
		@Schema(description = "When the user was last seen; null if never") Instant lastSeenAt) {
}
