package com.knowgauge.core.service.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.port.repository.TenantRepository;

class TenantAdminServiceImplTest {

	private final TenantRepository tenants = mock(TenantRepository.class);
	private final TenantAdminServiceImpl service = new TenantAdminServiceImpl(tenants);

	@Test
	void anUnknownSortPropertyIsRefusedBeforeTheQuery() {
		PageRequest page = PageRequest.of(0, 20, Sort.by("password"));

		assertThatThrownBy(() -> service.listTenants(new TenantSearch(null, null), page))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Sortable: createdAt, documents");
		verify(tenants, never()).findTenantSummaries(any(), any());
	}

	@Test
	void everySortableColumnReachesTheRepository() {
		for (String property : TenantSearch.SORTABLE) {
			PageRequest page = PageRequest.of(0, 20, Sort.by(property));
			service.listTenants(new TenantSearch(null, null), page);
			verify(tenants).findTenantSummaries(new TenantSearch(null, null), page);
		}
	}
}
