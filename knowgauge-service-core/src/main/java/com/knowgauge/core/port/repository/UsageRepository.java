package com.knowgauge.core.port.repository;

import java.time.Instant;
import java.util.List;

import com.knowgauge.core.model.ModelTokenUsage;
import com.knowgauge.core.model.UsageSummary;

/** Aggregate counts over a tenant's content and activity. */
public interface UsageRepository {

	/**
	 * @param tenantId the tenant whose own content is counted
	 * @param from     start of the activity period, inclusive
	 * @param to       end of the activity period, exclusive
	 */
	UsageSummary summarize(Long tenantId, Instant from, Instant to);

	/** LLM tokens of the tenant's calls in the period, per model name as the provider returned it. */
	List<ModelTokenUsage> tokensByModel(Long tenantId, Instant from, Instant to);

	/** Every model name recorded for an LLM call, across all tenants, by name. */
	List<String> findUsedModels();
}
