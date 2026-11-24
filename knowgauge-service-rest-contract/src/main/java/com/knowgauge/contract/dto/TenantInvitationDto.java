package com.knowgauge.contract.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An invitation to join a workspace (ADR015)")
public record TenantInvitationDto(
		@Schema(description = "Invitation id", example = "7") Long id,
		@Schema(description = "Workspace (tenant)", example = "12") Long tenantId,
		@Schema(description = "Invited address (lower-cased)", example = "ana@example.com") String email,
		@Schema(description = "Role the invitee gets on accepting", example = "TENANT_ADMIN") String role,
		@Schema(description = "The link to accept") String link,
		@Schema(description = "When it was invited (UTC)") Instant createdAt,
		@Schema(description = "Last successful send (UTC); empty if never delivered") Instant sentAt,
		@Schema(description = "How often a send was attempted", example = "1") Integer sendCount,
		@Schema(description = "Error of the last send; empty after a successful one") String sendError,
		@Schema(description = "When it was accepted (UTC); empty while pending") Instant acceptedAt) {
}
