package com.knowgauge.core.service.usage;

import com.knowgauge.core.model.UsageSummary;

/** What the current tenant holds and used this month. */
public interface UsageService {

	/** The current tenant's own content and its activity in the current calendar month (UTC). */
	UsageSummary currentMonth();

	/**
	 * Before new AI work (test generation, question regeneration): checks the current tenant's LLM tokens this month
	 * against the monthly limit (system setting). Work already running is not stopped, so one generation can go over.
	 *
	 * @throws com.knowgauge.core.exception.TokenLimitReachedException when the tenant used its allowance
	 */
	void requireTokensLeft();
}
