package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The signed-in user and the tenant they work in")
public record CurrentUserDto(
		@Schema(description = "Local user id; null when sign-in is off and kg.context.user-id is empty") Long userId,
		@Schema(description = "The one tenant the user belongs to") Long tenantId,
		String tenantName,
		@Schema(description = "PERSONAL, ORGANIZATION or LIBRARY", example = "PERSONAL") String tenantType,
		String email,
		String displayName,
		@Schema(description = "The user's roles from user_roles, e.g. ADMIN and TENANT_ADMIN (ADR012)") List<String> roles,
		@Schema(description = "fixed (no sign-in, one tenant per deployment) or oidc (Keycloak)") String securityMode,
		@Schema(description = "When the user first signed in; null when sign-in is off") Instant createdAt) {
}
