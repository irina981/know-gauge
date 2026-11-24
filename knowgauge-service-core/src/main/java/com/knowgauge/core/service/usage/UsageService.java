package com.knowgauge.core.service.usage;

import com.knowgauge.core.model.UsageSummary;

/** What the current tenant holds and used this month. */
public interface UsageService {

	/** The current tenant's own content and its activity in the current calendar month (UTC). */
	UsageSummary currentMonth();
}
