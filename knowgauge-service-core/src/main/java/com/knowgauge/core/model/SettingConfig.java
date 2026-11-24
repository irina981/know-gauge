package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.SettingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One possible setting key, a row of {@code setting_configs} (seeded by Flyway). Every stored {@link Setting}
 * references one. Each must match a {@link com.knowgauge.core.service.settings.SettingDefinition} with the same key
 * and type; the application checks that at startup.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingConfig {

	private String key;
	private SettingType type;
}
