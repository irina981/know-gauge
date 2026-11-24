package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.TenantType;

/**
 * A tenant as the admin list shows it: type, owner, how much content it holds (ADR013), the storage its documents
 * take and its LLM tokens this month, each against its limit (system settings; {@code null} when there is none).
 */
public record TenantSummary(Long tenantId, String name, TenantType type, Long ownerUserId, String ownerName,
		String ownerEmail, Instant createdAt, long topics, long documents, long storageBytes, Long storageLimitBytes,
		long tokensThisMonth, Long tokenLimit) {

	/** For the repository query, which knows the amounts but not the limits. */
	public TenantSummary(Long tenantId, String name, TenantType type, Long ownerUserId, String ownerName,
			String ownerEmail, Instant createdAt, long topics, long documents, long storageBytes,
			long tokensThisMonth) {
		this(tenantId, name, type, ownerUserId, ownerName, ownerEmail, createdAt, topics, documents, storageBytes, null,
				tokensThisMonth, null);
	}

	/** The limits every tenant shares; the library has no storage limit. */
	public TenantSummary withLimits(Long storageLimit, Long tokenLimit) {
		return new TenantSummary(tenantId, name, type, ownerUserId, ownerName, ownerEmail, createdAt, topics, documents,
				storageBytes, type == TenantType.LIBRARY ? null : storageLimit, tokensThisMonth, tokenLimit);
	}
}
