package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.TenantType;

/** A tenant as the admin list shows it: type, owner and how much content it holds (ADR013). */
public record TenantSummary(Long tenantId, String name, TenantType type, Long ownerUserId, String ownerName,
		String ownerEmail, Instant createdAt, long topics, long documents) {
}
