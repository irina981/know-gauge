package com.knowgauge.core.service.usage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.UsageSummary;
import com.knowgauge.core.port.repository.UsageRepository;

@Service
public class UsageServiceImpl implements UsageService {

	private final UsageRepository usageRepository;
	private final ExecutionContext executionContext;
	private final Clock clock;

	@Autowired
	public UsageServiceImpl(UsageRepository usageRepository, ExecutionContext executionContext) {
		this(usageRepository, executionContext, Clock.systemUTC());
	}

	UsageServiceImpl(UsageRepository usageRepository, ExecutionContext executionContext, Clock clock) {
		this.usageRepository = usageRepository;
		this.executionContext = executionContext;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public UsageSummary currentMonth() {
		LocalDate firstDay = LocalDate.now(clock.withZone(ZoneOffset.UTC)).withDayOfMonth(1);
		Instant from = firstDay.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant to = firstDay.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		return usageRepository.summarize(executionContext.tenantId(), from, to);
	}
}
