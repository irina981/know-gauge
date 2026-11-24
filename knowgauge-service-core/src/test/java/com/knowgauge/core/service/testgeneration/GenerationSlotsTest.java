package com.knowgauge.core.service.testgeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.exception.GenerationLimitReachedException;
import com.knowgauge.core.service.settings.SystemSettingsService;

/** Each user may generate as many tests at once as the system setting allows. */
class GenerationSlotsTest {

	private final SystemSettingsService settings = mock(SystemSettingsService.class);
	private final GenerationSlots slots = new GenerationSlots(settings);

	@Test
	void aSecondGenerationOfTheSameUserIsRefusedUntilTheFirstEnds() {
		when(settings.concurrentGenerationsPerUser()).thenReturn(Optional.of(1));

		GenerationSlots.Slot first = slots.acquire(7L);
		assertThatThrownBy(() -> slots.acquire(7L)).isInstanceOf(GenerationLimitReachedException.class)
				.hasMessageContaining("still being generated");

		first.close();
		try (GenerationSlots.Slot again = slots.acquire(7L)) {
			assertThat(slots.running(7L)).isEqualTo(1);
		}
		assertThat(slots.running(7L)).isZero();
	}

	@Test
	void otherUsersHaveSlotsOfTheirOwn() {
		when(settings.concurrentGenerationsPerUser()).thenReturn(Optional.of(1));

		try (GenerationSlots.Slot ana = slots.acquire(7L); GenerationSlots.Slot bob = slots.acquire(8L)) {
			assertThat(slots.running(7L)).isEqualTo(1);
			assertThat(slots.running(8L)).isEqualTo(1);
		}
	}

	@Test
	void aHigherLimitAllowsThatManyAtOnce() {
		when(settings.concurrentGenerationsPerUser()).thenReturn(Optional.of(2));

		try (GenerationSlots.Slot one = slots.acquire(7L); GenerationSlots.Slot two = slots.acquire(7L)) {
			assertThatThrownBy(() -> slots.acquire(7L)).isInstanceOf(GenerationLimitReachedException.class)
					.hasMessageContaining("already generating 2 tests");
		}
	}

	@Test
	void withoutALimitNothingIsCounted() {
		when(settings.concurrentGenerationsPerUser()).thenReturn(Optional.empty());

		try (GenerationSlots.Slot one = slots.acquire(7L); GenerationSlots.Slot two = slots.acquire(7L)) {
			assertThat(slots.running(7L)).isZero();
		}
	}
}
