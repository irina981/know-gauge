package com.knowgauge.core.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowgauge.core.model.WorkspaceLimits;
import com.knowgauge.core.service.settings.TenantSettingsService;

/** {@link TenantSettingsService}s for unit tests. */
public final class TenantSettings {

	private TenantSettings() {
	}

	/** Every workspace has the given limits. */
	public static TenantSettingsService withLimits(WorkspaceLimits limits) {
		TenantSettingsService settings = mock(TenantSettingsService.class);
		when(settings.limits(any(Long.class))).thenReturn(limits);
		return settings;
	}

	/** No workspace has any limit. */
	public static TenantSettingsService noLimits() {
		return withLimits(WorkspaceLimits.NONE);
	}
}
