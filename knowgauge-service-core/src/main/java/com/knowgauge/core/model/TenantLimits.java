package com.knowgauge.core.model;

import java.util.List;
import java.util.function.Function;

/**
 * The limits in effect for one tenant: its own value if it has one, else the system value (ADR014).
 * {@code null} means no limit; a stored 0 means no limit too.
 *
 * @param monthlyTokens         LLM tokens (input + output) per calendar month (UTC)
 * @param storageBytes          bytes of uploaded documents
 * @param concurrentGenerations tests one user may generate at the same time
 * @param participantsPerTest   different people one test may have: invited or started an attempt
 */
public record TenantLimits(Long monthlyTokens, Long storageBytes, Integer concurrentGenerations,
		Integer participantsPerTest) {

	public static final String MONTHLY_TOKENS = "kg.settings.tenant.monthly-token-limit";
	public static final String STORAGE_MB = "kg.settings.tenant.storage-limit-mb";
	public static final String CONCURRENT_GENERATIONS = "kg.settings.tenant.concurrent-generations-per-user";
	public static final String PARTICIPANTS_PER_TEST = "kg.settings.tenant.participants-per-test";

	/** The setting keys of the limits. */
	public static final List<String> KEYS = List.of(MONTHLY_TOKENS, STORAGE_MB, CONCURRENT_GENERATIONS,
			PARTICIPANTS_PER_TEST);

	/** The value that stands for "no limit". */
	public static final String NO_LIMIT = "0";

	public static final TenantLimits NONE = new TenantLimits(null, null, null, null);

	/**
	 * @param valueOf the value in effect of a limit key, as text ({@code null} or "0" for no limit)
	 */
	public static TenantLimits of(Function<String, String> valueOf) {
		Long storageMb = limit(valueOf.apply(STORAGE_MB));
		Long concurrent = limit(valueOf.apply(CONCURRENT_GENERATIONS));
		Long participants = limit(valueOf.apply(PARTICIPANTS_PER_TEST));
		return new TenantLimits(limit(valueOf.apply(MONTHLY_TOKENS)),
				storageMb == null ? null : storageMb * 1024 * 1024,
				concurrent == null ? null : concurrent.intValue(),
				participants == null ? null : participants.intValue());
	}

	private static Long limit(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		long value = Long.parseLong(text.trim());
		return value <= 0 ? null : value;
	}
}
