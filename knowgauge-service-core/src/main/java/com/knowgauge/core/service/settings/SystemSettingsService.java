package com.knowgauge.core.service.settings;

import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.SettingConfig;
import com.knowgauge.core.model.enums.SettingOwner;

/**
 * System-wide settings an {@code ADMIN} keeps on the System settings page. Each starts from its properties-file value;
 * a {@code SYSTEM} row in the database overrides it. At startup every saved value is applied; a change applies at once.
 * Which settings may have a system-wide or a per-tenant value, and their type, comes from the database
 * ({@code setting_configs}, {@code setting_config_owners}; ADR014).
 */
public interface SystemSettingsService {

	/**
	 * A setting with its type and owners from the database, its properties-file default, whether an override is
	 * saved (its value may be empty, e.g. no limit), and the value in effect.
	 */
	record SystemSettingValue(SettingDefinition definition, SettingConfig config, String defaultValue,
			boolean overridden, String overrideValue, String value, List<String> suggestions) {
	}

	/** @return every setting that may have a system-wide value ({@code SYSTEM} owner), in page order */
	List<SystemSettingValue> list();

	/**
	 * Saves overrides (the System settings page's Save) and applies them at once. All values are checked first; if
	 * one does not fit, nothing is saved.
	 *
	 * @param values new values by key; an empty value means empty (optional settings only, e.g. no limit)
	 * @return every setting afterwards
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException for an unknown key
	 * @throws IllegalArgumentException                               naming every value that does not fit, or for a
	 *                                                                key without a {@code SYSTEM} owner
	 */
	List<SystemSettingValue> setAll(Map<String, String> values);

	/**
	 * Removes every override from the database (the page's Reset to defaults), so the properties values apply again.
	 *
	 * @return every setting afterwards
	 */
	List<SystemSettingValue> resetAll();

	/** @return every setting's definition, in page order */
	List<SettingDefinition> definitions();

	/** @return the definitions of the settings the given owner may have a value of, in page order */
	List<SettingDefinition> definitions(SettingOwner owner);

	/**
	 * @return a defined setting's type and owners, as the database says
	 * @throws IllegalStateException for an undefined key, or before the settings are loaded at startup
	 */
	SettingConfig config(String key);
}
