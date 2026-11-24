package com.knowgauge.core.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;

import com.knowgauge.core.model.SettingConfig;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.core.port.repository.SettingConfigRepository;
import com.knowgauge.core.service.settings.SettingDefinition;

/** {@code setting_configs} / {@code setting_config_owners} for unit tests, as Flyway seeds them (V7, V8, V9). */
public final class SettingConfigs {

	public static final String INVITATIONS_AT_ONCE = "kg.invitations.max-per-request";

	private SettingConfigs() {
	}

	/**
	 * The seeded row of each definition: every key has a system value; the workspace limits ({@link TenantLimits#KEYS})
	 * may also have one per tenant; they and the invitations sent at once are limits, the rest configuration.
	 */
	public static SettingConfig seeded(SettingDefinition definition) {
		String key = definition.key();
		boolean tenantLimit = TenantLimits.KEYS.contains(key);
		return new SettingConfig(key,
				tenantLimit || INVITATIONS_AT_ONCE.equals(key) ? SettingType.LIMIT : SettingType.CONFIG,
				tenantLimit ? EnumSet.of(SettingOwner.SYSTEM, SettingOwner.TENANT) : EnumSet.of(SettingOwner.SYSTEM));
	}

	/** A repository with the seeded row of each definition. */
	public static SettingConfigRepository seededFor(List<SettingDefinition> definitions) {
		return returning(definitions.stream().map(SettingConfigs::seeded).toList());
	}

	/** A repository with exactly the given rows. */
	public static SettingConfigRepository returning(List<SettingConfig> configs) {
		SettingConfigRepository repository = mock(SettingConfigRepository.class);
		when(repository.findAll()).thenReturn(configs);
		return repository;
	}
}
