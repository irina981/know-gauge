package com.knowgauge.core.service.usage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.TokenLimitReachedException;
import com.knowgauge.core.model.ModelTokenUsage;
import com.knowgauge.core.port.repository.TokenPriceRepository;
import com.knowgauge.core.port.repository.UsageRepository;
import com.knowgauge.core.service.settings.SystemSettingsService;

class UsageServiceImplTest {

	private static final long TENANT = 7L;
	private static final Instant NOV_1 = Instant.parse("2026-11-01T00:00:00Z");
	private static final Instant DEC_1 = Instant.parse("2026-12-01T00:00:00Z");

	private final UsageRepository usageRepository = mock(UsageRepository.class);
	private final SystemSettingsService settings = mock(SystemSettingsService.class);
	private final ExecutionContext ctx = mock(ExecutionContext.class);
	private UsageServiceImpl service;

	@BeforeEach
	void setUp() {
		when(ctx.tenantId()).thenReturn(TENANT);
		service = new UsageServiceImpl(usageRepository, mock(TokenPriceRepository.class), settings, ctx,
				Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void withoutALimitNothingIsCounted() {
		when(settings.monthlyTokenLimit()).thenReturn(Optional.empty());

		assertThatCode(service::requireTokensLeft).doesNotThrowAnyException();
		verify(usageRepository, never()).tokensByModel(any(), any(), any());
	}

	@Test
	void belowTheLimitWorkMayStart() {
		when(settings.monthlyTokenLimit()).thenReturn(Optional.of(1_000L));
		when(usageRepository.tokensByModel(eq(TENANT), eq(NOV_1), eq(DEC_1)))
				.thenReturn(List.of(new ModelTokenUsage("gpt-4.1", 600, 399)));

		assertThatCode(service::requireTokensLeft).doesNotThrowAnyException();
	}

	@Test
	void atTheLimitNewWorkIsRefused() {
		when(settings.monthlyTokenLimit()).thenReturn(Optional.of(1_000L));
		when(usageRepository.tokensByModel(eq(TENANT), eq(NOV_1), eq(DEC_1)))
				.thenReturn(List.of(new ModelTokenUsage("gpt-4.1", 600, 300), new ModelTokenUsage("gpt-5", 50, 50)));

		assertThatThrownBy(service::requireTokensLeft).isInstanceOf(TokenLimitReachedException.class)
				.hasMessageContaining("1,000 AI tokens");
	}
}
