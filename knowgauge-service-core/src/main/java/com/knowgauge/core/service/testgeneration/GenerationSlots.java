package com.knowgauge.core.service.testgeneration;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.GenerationLimitReachedException;
import com.knowgauge.core.service.settings.SystemSettingsService;

import lombok.extern.slf4j.Slf4j;

/**
 * How many tests each user is generating right now, against the limit (system setting). Kept in memory, not in the
 * database: a generation that dies with the server cannot leave its user blocked. The count is per server, which is
 * enough while KnowGauge runs as a single instance.
 */
@Slf4j
@Component
public class GenerationSlots {

	/** A held slot; closing it gives it back. */
	public interface Slot extends AutoCloseable {
		@Override
		void close();
	}

	private static final Slot UNCOUNTED = () -> {
	};

	private final SystemSettingsService systemSettings;
	private final Map<Long, Integer> running = new ConcurrentHashMap<>();

	public GenerationSlots(SystemSettingsService systemSettings) {
		this.systemSettings = systemSettings;
	}

	/**
	 * Takes one of the user's generation slots.
	 *
	 * @param userId the user starting a generation; {@code null} (no user) is not counted
	 * @throws GenerationLimitReachedException when the user already runs as many generations as allowed
	 */
	public Slot acquire(Long userId) {
		Optional<Integer> limit = systemSettings.concurrentGenerationsPerUser();
		if (userId == null || limit.isEmpty()) {
			return UNCOUNTED;
		}
		int max = limit.get();
		boolean[] refused = { false };
		running.compute(userId, (id, count) -> {
			int current = count == null ? 0 : count;
			if (current >= max) {
				refused[0] = true;
				return count;
			}
			return current + 1;
		});
		if (refused[0]) {
			log.info("User {} refused a new test generation: already running {} (the limit)", userId, max);
			throw new GenerationLimitReachedException(max == 1
					? "A test you started is still being generated. Wait until it is ready, then start the next one."
					: String.format(Locale.ROOT,
							"You are already generating %d tests, the most at once. Wait until one is ready, then"
									+ " start the next one.",
							max));
		}
		return () -> running.computeIfPresent(userId, (id, count) -> count <= 1 ? null : count - 1);
	}

	/** @return the user's generations running now */
	int running(Long userId) {
		return running.getOrDefault(userId, 0);
	}
}
