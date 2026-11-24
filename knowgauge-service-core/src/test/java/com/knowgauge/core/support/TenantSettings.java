package com.knowgauge.core.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.service.settings.TenantSettingsService;

/** {@link TenantSettingsService}s for unit tests. */
public final class TenantSettings {

	private TenantSettings() {
	}

	/** Every tenant has the given limits. */
	public static TenantSettingsService withLimits(TenantLimits limits) {
		TenantSettingsService settings = mock(TenantSettingsService.class);
		when(settings.limits(any(Long.class))).thenReturn(limits);
		return settings;
	}

	/** No tenant has any limit. */
	public static TenantSettingsService noLimits() {
		return withLimits(TenantLimits.NONE);
	}
}
