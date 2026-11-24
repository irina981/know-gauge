package com.knowgauge.core.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;

class TenantAdminServiceImplTest {

	private static final Instant NOV_1 = Instant.parse("2026-11-01T00:00:00Z");
	private static final Instant DEC_1 = Instant.parse("2026-12-01T00:00:00Z");

	private final TenantRepository tenants = mock(TenantRepository.class);
	private final TenantSettingsService settings = mock(TenantSettingsService.class);
	private final TenantAdminServiceImpl service = new TenantAdminServiceImpl(tenants, settings,
			Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC));

	@BeforeEach
	void setUp() {
		when(tenants.findTenantSummaries(any(), any(), any(), any())).thenReturn(Page.empty());
	}

	@Test
	void anUnknownSortPropertyIsRefusedBeforeTheQuery() {
		PageRequest page = PageRequest.of(0, 20, Sort.by("password"));

		assertThatThrownBy(() -> service.listTenants(new TenantSearch(null, null), page))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Sortable: createdAt, documents");
		verify(tenants, never()).findTenantSummaries(any(), any(), any(), any());
	}

	@Test
	void everySortableColumnReachesTheRepository() {
		for (String property : TenantSearch.SORTABLE) {
			PageRequest page = PageRequest.of(0, 20, Sort.by(property));
			service.listTenants(new TenantSearch(null, null), page);
			verify(tenants).findTenantSummaries(eq(new TenantSearch(null, null)), eq(page), any(), any());
		}
	}

	@Test
	void tokensAreCountedForTheCurrentMonthAndEachTenantsLimitsAreAttached() {
		PageRequest page = PageRequest.of(0, 20);
		TenantSummary row = new TenantSummary(3L, "Ana", TenantType.INDIVIDUAL, 2L, "Ana", "ana@x.test", NOV_1, 1, 2,
				3_000L, 1_200L);
		when(tenants.findTenantSummaries(any(), eq(page), eq(NOV_1), eq(DEC_1)))
				.thenReturn(new PageImpl<>(List.of(row), page, 1));
		when(settings.limits(List.of(3L))).thenReturn(Map.of(3L, new TenantLimits(1_000L, 2_000L, 2)));
		when(settings.withOwnValues(List.of(3L))).thenReturn(Set.of(3L));

		Page<TenantSummary> result = service.listTenants(new TenantSearch(null, null), page);

		assertThat(result.getContent()).singleElement().satisfies(s -> {
			assertThat(s.tokensThisMonth()).isEqualTo(1_200L);
			assertThat(s.tokenLimit()).isEqualTo(1_000L);
			assertThat(s.storageBytes()).isEqualTo(3_000L);
			assertThat(s.storageLimitBytes()).isEqualTo(2_000L);
			assertThat(s.concurrentGenerationsLimit()).isEqualTo(2);
			assertThat(s.ownLimits()).isTrue();
		});
	}
}
