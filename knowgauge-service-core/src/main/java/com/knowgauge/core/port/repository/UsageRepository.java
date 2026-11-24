package com.knowgauge.core.port.repository;

import java.time.Instant;

import com.knowgauge.core.model.UsageSummary;

/** Aggregate counts over a tenant's content and activity. */
public interface UsageRepository {

	/**
	 * @param tenantId the tenant whose own content is counted
	 * @param from     start of the activity period, inclusive
	 * @param to       end of the activity period, exclusive
	 */
	UsageSummary summarize(Long tenantId, Instant from, Instant to);
}
