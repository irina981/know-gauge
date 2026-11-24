package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The workspace the user joined by accepting an invitation")
public record TenantJoinedDto(
		@Schema(description = "The workspace (tenant) the user now works in", example = "12") Long tenantId,
		@Schema(description = "Its name", example = "Acme Training") String tenantName,
		@Schema(description = "The workspace the user left; empty if none") Long previousTenantId) {
}
