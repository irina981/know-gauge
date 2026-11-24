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
		long documents,
		@Schema(description = "Total size of the tenant's documents in bytes") long storageBytes,
		@Schema(description = "Bytes of documents the tenant may keep (its own limit, else the system one); null when there is none (always for the library). Over it uploads are refused") Long storageLimitBytes,
		@Schema(description = "LLM tokens (input + output) of the tenant this calendar month (UTC)") long tokensThisMonth,
		@Schema(description = "LLM tokens the tenant may use per month (its own limit, else the system one); null when there is none. At the limit new tests and regenerations are refused") Long tokenLimit,
		@Schema(description = "Tests one user of the tenant may generate at once; null when there is no limit") Integer concurrentGenerationsLimit,
		@Schema(description = "Whether the tenant has limits of its own, overriding the system settings (ADR014)") boolean ownLimits) {
}
