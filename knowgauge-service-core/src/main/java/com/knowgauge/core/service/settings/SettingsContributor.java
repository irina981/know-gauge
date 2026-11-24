package com.knowgauge.core.service.settings;

import java.util.List;

/**
 * A module's settings for the System settings page. Every bean implementing this is picked up by
 * {@link SystemSettingsServiceImpl}, so an infra module can add settings of its own properties without core knowing
 * them.
 */
public interface SettingsContributor {

	List<SettingDefinition> definitions();
}
