package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.TenantType;

/**
 * A tenant as the admin list shows it: type, owner, how much content it holds (ADR013), the storage its documents
 * take and its LLM tokens this month, each against the limit in effect for it ({@code null} when there is none), and
 * whether it has limits of its own (ADR014).
 */
public record TenantSummary(Long tenantId, String name, TenantType type, Long ownerUserId, String ownerName,
		String ownerEmail, Instant createdAt, long topics, long documents, long storageBytes, Long storageLimitBytes,
		long tokensThisMonth, Long tokenLimit, Integer concurrentGenerationsLimit, boolean ownLimits) {

	/** For the repository query, which knows the amounts but not the limits. */
	public TenantSummary(Long tenantId, String name, TenantType type, Long ownerUserId, String ownerName,
			String ownerEmail, Instant createdAt, long topics, long documents, long storageBytes,
			long tokensThisMonth) {
		this(tenantId, name, type, ownerUserId, ownerName, ownerEmail, createdAt, topics, documents, storageBytes, null,
				tokensThisMonth, null, null, false);
	}

	/** This tenant with its limits in effect (ADR014). */
	public TenantSummary withLimits(TenantLimits limits, boolean ownLimits) {
		return new TenantSummary(tenantId, name, type, ownerUserId, ownerName, ownerEmail, createdAt, topics, documents,
				storageBytes, limits.storageBytes(), tokensThisMonth, limits.monthlyTokens(),
				limits.concurrentGenerations(), ownLimits);
	}
}
