package com.knowgauge.core.service.usage;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * The period usage is counted in: a calendar month, UTC. Shared by the usage page, the monthly token limit and the
 * admin workspace list, so they all count the same tokens.
 *
 * @param from start, inclusive
 * @param to   end, exclusive
 */
public record UsagePeriod(Instant from, Instant to) {

	public static UsagePeriod currentMonth(Clock clock) {
		LocalDate firstDay = LocalDate.now(clock.withZone(ZoneOffset.UTC)).withDayOfMonth(1);
		return new UsagePeriod(firstDay.atStartOfDay(ZoneOffset.UTC).toInstant(),
				firstDay.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant());
	}
}
