package com.knowgauge.core.service.testgeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.exception.GenerationLimitReachedException;
import com.knowgauge.core.model.WorkspaceLimits;
import com.knowgauge.core.service.settings.TenantSettingsService;

/** Each user may generate as many tests at once as their workspace's limit allows. */
class GenerationSlotsTest {

	private static final long WORKSPACE = 3L;

	private final TenantSettingsService settings = mock(TenantSettingsService.class);
	private final GenerationSlots slots = new GenerationSlots(settings);

	@Test
	void aSecondGenerationOfTheSameUserIsRefusedUntilTheFirstEnds() {
		when(settings.limits(WORKSPACE)).thenReturn(new WorkspaceLimits(null, null, 1));

		GenerationSlots.Slot first = slots.acquire(7L, WORKSPACE);
		assertThatThrownBy(() -> slots.acquire(7L, WORKSPACE)).isInstanceOf(GenerationLimitReachedException.class)
				.hasMessageContaining("still being generated");

		first.close();
		try (GenerationSlots.Slot again = slots.acquire(7L, WORKSPACE)) {
			assertThat(slots.running(7L)).isEqualTo(1);
		}
		assertThat(slots.running(7L)).isZero();
	}

	@Test
	void otherUsersHaveSlotsOfTheirOwn() {
		when(settings.limits(WORKSPACE)).thenReturn(new WorkspaceLimits(null, null, 1));

		try (GenerationSlots.Slot ana = slots.acquire(7L, WORKSPACE);
				GenerationSlots.Slot bob = slots.acquire(8L, WORKSPACE)) {
			assertThat(slots.running(7L)).isEqualTo(1);
			assertThat(slots.running(8L)).isEqualTo(1);
		}
	}

	@Test
	void aHigherLimitAllowsThatManyAtOnce() {
		when(settings.limits(WORKSPACE)).thenReturn(new WorkspaceLimits(null, null, 2));

		try (GenerationSlots.Slot one = slots.acquire(7L, WORKSPACE);
				GenerationSlots.Slot two = slots.acquire(7L, WORKSPACE)) {
			assertThatThrownBy(() -> slots.acquire(7L, WORKSPACE)).isInstanceOf(GenerationLimitReachedException.class)
					.hasMessageContaining("already generating 2 tests");
		}
	}

	@Test
	void withoutALimitNothingIsCounted() {
		when(settings.limits(WORKSPACE)).thenReturn(WorkspaceLimits.NONE);

		try (GenerationSlots.Slot one = slots.acquire(7L, WORKSPACE);
				GenerationSlots.Slot two = slots.acquire(7L, WORKSPACE)) {
			assertThat(slots.running(7L)).isZero();
		}
	}
}
