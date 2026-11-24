package com.knowgauge.core.service.settings;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * System-wide settings an {@code ADMIN} keeps on the System settings page. Each starts from its properties-file value;
 * a value saved in the database overrides it. At startup every saved value is applied; a change applies at once.
 */
public interface SystemSettingsService {

	/**
	 * A setting with its properties-file default, whether an override is saved (its value may be empty, e.g. no
	 * limit), and the value in effect.
	 */
	record SystemSettingValue(SystemSettingDefinition definition, String defaultValue, boolean overridden,
			String overrideValue, String value, List<String> suggestions) {
	}

	/** @return every setting, in page order */
	List<SystemSettingValue> list();

	/**
	 * Saves overrides (the System settings page's Save) and applies them at once. All values are checked first; if
	 * one does not fit, nothing is saved.
	 *
	 * @param values new values by key; an empty value means empty (optional settings only, e.g. no limit)
	 * @return every setting afterwards
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException for an unknown key
	 * @throws IllegalArgumentException                               naming every value that does not fit
	 */
	List<SystemSettingValue> setAll(Map<String, String> values);

	/**
	 * Removes every override from the database (the page's Reset to defaults), so the properties values apply again.
	 *
	 * @return every setting afterwards
	 */
	List<SystemSettingValue> resetAll();

	/** @return the LLM tokens each tenant may use per calendar month; empty when there is no limit */
	Optional<Long> monthlyTokenLimit();

	/** @return the bytes of documents each tenant may keep; empty when there is no limit */
	Optional<Long> storageLimitBytes();

	/** @return the tests one user may generate at the same time; empty when there is no limit */
	Optional<Integer> concurrentGenerationsPerUser();
}
