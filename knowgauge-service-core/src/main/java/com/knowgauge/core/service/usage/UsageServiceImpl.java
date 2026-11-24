package com.knowgauge.core.service.usage;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.TokenLimitReachedException;
import com.knowgauge.core.model.UsageSummary;
import com.knowgauge.core.port.repository.TokenPriceRepository;
import com.knowgauge.core.port.repository.UsageRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.core.model.WorkspaceLimits;

@Slf4j
@Service
public class UsageServiceImpl implements UsageService {

	private final UsageRepository usageRepository;
	private final TokenPriceRepository tokenPriceRepository;
	private final TenantSettingsService tenantSettings;
	private final ExecutionContext executionContext;
	private final Clock clock;

	@Autowired
	public UsageServiceImpl(UsageRepository usageRepository, TokenPriceRepository tokenPriceRepository,
			TenantSettingsService tenantSettings, ExecutionContext executionContext) {
		this(usageRepository, tokenPriceRepository, tenantSettings, executionContext, Clock.systemUTC());
	}

	UsageServiceImpl(UsageRepository usageRepository, TokenPriceRepository tokenPriceRepository,
			TenantSettingsService tenantSettings, ExecutionContext executionContext, Clock clock) {
		this.usageRepository = usageRepository;
		this.tokenPriceRepository = tokenPriceRepository;
		this.tenantSettings = tenantSettings;
		this.executionContext = executionContext;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public void requireTokensLeft() {
		Long tenantId = executionContext.tenantId();
		Optional<Long> limit = Optional.ofNullable(tenantSettings.limits(tenantId).monthlyTokens());
		if (limit.isEmpty()) {
			return;
		}
		Instant[] month = currentMonthRange();
		long used = usageRepository.tokensByModel(tenantId, month[0], month[1]).stream()
				.mapToLong(u -> u.inputTokens() + u.outputTokens()).sum();
		if (used >= limit.get()) {
			log.info("Tenant {} reached its monthly token limit: {} of {} tokens used", tenantId, used, limit.get());
			throw new TokenLimitReachedException(String.format(Locale.ROOT,
					"Your workspace used its %,d AI tokens for this month (%,d used). New tests and regenerations are"
							+ " possible again on the first day of next month (UTC).",
					limit.get(), used));
		}
	}

	/** Start (inclusive) and end (exclusive) of the current calendar month, UTC. */
	private Instant[] currentMonthRange() {
		UsagePeriod month = UsagePeriod.currentMonth(clock);
		return new Instant[] { month.from(), month.to() };
	}

	@Override
	@Transactional(readOnly = true)
	public UsageSummary currentMonth() {
		Instant[] month = currentMonthRange();
		Instant from = month[0];
		Instant to = month[1];
		Long tenantId = executionContext.tenantId();
		TokenPricing.Cost cost = TokenPricing.cost(usageRepository.tokensByModel(tenantId, from, to),
				tokenPriceRepository.findAll());
		WorkspaceLimits limits = tenantSettings.limits(tenantId);
		return usageRepository.summarize(tenantId, from, to).toBuilder().llmCostUsd(cost.costUsd())
				.unpricedModels(cost.unpricedModels()).llmTokenLimit(limits.monthlyTokens())
				.storageLimitBytes(limits.storageBytes())
				.build();
	}
}
