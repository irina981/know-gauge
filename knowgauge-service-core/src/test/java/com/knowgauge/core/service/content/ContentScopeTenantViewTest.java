package com.knowgauge.core.service.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;

/** An ADMIN viewing another user's library (tenant) reads it, but nothing in it is writable (ADR013). */
class ContentScopeTenantViewTest {

	private static final long VIEWED_TENANT = 7L;

	@Test
	void theViewedTenantIsReadableButNotWritable() {
		ExecutionContext context = mock(ExecutionContext.class);
		// While viewing, the current tenant is the viewed tenant.
		when(context.tenantId()).thenReturn(VIEWED_TENANT);
		TenantRepository tenants = mock(TenantRepository.class);
		when(tenants.findIdsByType(TenantType.LIBRARY)).thenReturn(List.of(1L));

		ContentScope viewing = new ContentScope(tenants, context, () -> true, () -> true);
		ContentScope own = new ContentScope(tenants, context, () -> true, () -> false);

		assertThat(viewing.readableTenantIds()).containsExactly(VIEWED_TENANT, 1L);
		assertThat(viewing.canWrite(VIEWED_TENANT)).isFalse();
		assertThat(viewing.canWrite(1L)).isFalse();
		assertThat(own.canWrite(VIEWED_TENANT)).isTrue();
	}
}
