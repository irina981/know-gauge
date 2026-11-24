package com.knowgauge.core.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.service.content.ContentScope;

/** {@link ContentScope}s for unit tests. */
public final class ContentScopes {

	private ContentScopes() {
	}

	/** No library tenants: the current tenant reads and writes only its own content. */
	public static ContentScope ownTenantOnly(ExecutionContext context) {
		return withLibraries(context, false);
	}

	/** The given library tenants, read-only unless {@code applicationAdmin}. */
	public static ContentScope withLibraries(ExecutionContext context, boolean applicationAdmin, Long... libraryIds) {
		TenantRepository tenants = mock(TenantRepository.class);
		when(tenants.findIdsByType(TenantType.LIBRARY)).thenReturn(List.of(libraryIds));
		return new ContentScope(tenants, context, () -> applicationAdmin, () -> false, Optional::empty);
	}
}
