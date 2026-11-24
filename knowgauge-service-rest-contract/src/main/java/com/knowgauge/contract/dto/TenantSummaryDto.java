package com.knowgauge.contract.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A tenant for admins: type, owner and how much content it holds (ADR013)")
public record TenantSummaryDto(
		@Schema(description = "Tenant id; send it in the X-KG-Tenant header to read this tenant's content") Long tenantId,
		String name,
		@Schema(allowableValues = { "INDIVIDUAL", "ORGANIZATION", "LIBRARY" }) String type,
		Long ownerUserId,
		String ownerName,
		String ownerEmail,
		Instant createdAt,
		long topics,
		long documents) {
}
