package com.knowgauge.core.service.settings;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.knowgauge.core.model.enums.SettingType;

/**
 * One setting an {@code ADMIN} may change. Its default comes from the properties files (bound into a
 * {@code @ConfigurationProperties} bean); a {@code SYSTEM} row in {@code settings} under the same {@code key}
 * overrides it (System settings page), and for a {@link #tenantLevel() tenant-level} setting a {@code TENANT}
 * row overrides that for one tenant (ADR014). Only settings read on each use belong here, so a change applies
 * without a restart. Every key needs a row in {@code setting_configs} (a Flyway migration) with the same
 * {@link #settingType()}; startup fails otherwise.
 *
 * @param key            the property name, also the {@code settings} key, e.g.
 *                       {@code kg.testgen.defaults.generation-model}
 * @param options        the allowed values of a {@link Type#CHOICE}, else empty
 * @param min            smallest allowed {@link Type#INTEGER}, or {@code null}
 * @param max            largest allowed {@link Type#INTEGER}, or {@code null}
 * @param optional       whether the setting may be empty
 * @param reader         the system value in effect, as text ({@code null} when empty)
 * @param writer         puts a valid system value (or {@code null} for empty) into the properties bean
 * @param suggestions    values to offer for a {@link Type#TEXT} (e.g. model names from the price catalog); any other
 *                       text is allowed too
 * @param tenantLevel    whether a tenant may have a value of its own (a {@code TENANT} row)
 * @param settingType    a configuration value or a limit ({@code setting_configs.setting_type}); the System
 *                       settings page shows each type on a tab of its own
 */
public record SettingDefinition(String key, String group, String label, String description, Type type,
		List<String> options, Long min, Long max, boolean optional, Supplier<String> reader, Consumer<String> writer,
		Supplier<List<String>> suggestions, boolean tenantLevel, SettingType settingType) {

	/** A system-only {@link SettingType#CONFIG} setting. */
	public SettingDefinition(String key, String group, String label, String description, Type type,
			List<String> options, Long min, Long max, boolean optional, Supplier<String> reader, Consumer<String> writer,
			Supplier<List<String>> suggestions) {
		this(key, group, label, description, type, options, min, max, optional, reader, writer, suggestions, false,
				SettingType.CONFIG);
	}

	/** A system-only {@link SettingType#CONFIG} setting without suggestions. */
	public SettingDefinition(String key, String group, String label, String description, Type type,
			List<String> options, Long min, Long max, boolean optional, Supplier<String> reader,
			Consumer<String> writer) {
		this(key, group, label, description, type, options, min, max, optional, reader, writer, List::of);
	}

	/** This setting, which a tenant may also have a value of its own for. */
	public SettingDefinition perTenant() {
		return new SettingDefinition(key, group, label, description, type, options, min, max, optional, reader, writer,
				suggestions, true, settingType);
	}

	/** This setting as a {@link SettingType#LIMIT} rather than a configuration value. */
	public SettingDefinition limit() {
		return new SettingDefinition(key, group, label, description, type, options, min, max, optional, reader, writer,
				suggestions, tenantLevel, SettingType.LIMIT);
	}

	public enum Type {
		INTEGER, TEXT, CHOICE
	}

	/**
	 * Checks a value and returns it trimmed, or {@code null} for an empty value of an optional setting.
	 *
	 * @throws IllegalArgumentException when the value does not fit the setting
	 */
	public String validate(String value) {
		String text = value == null ? null : value.trim();
		if (text == null || text.isEmpty()) {
			if (optional) {
				return null;
			}
			throw new IllegalArgumentException(label + " needs a value.");
		}
		switch (type) {
		case INTEGER -> {
			long number;
			try {
				number = Long.parseLong(text);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(label + " must be a whole number.");
			}
			if ((min != null && number < min) || (max != null && number > max)) {
				throw new IllegalArgumentException(label + " must be "
						+ (max == null ? "at least " + min : "between " + min + " and " + max) + ".");
			}
			return Long.toString(number);
		}
		case CHOICE -> {
			return options.stream().filter(option -> option.equalsIgnoreCase(text)).findFirst()
					.orElseThrow(() -> new IllegalArgumentException(
							label + " must be one of " + String.join(", ", options) + "."));
		}
		default -> {
			if (text.length() > 1000) {
				throw new IllegalArgumentException(label + " may have at most 1000 characters.");
			}
			return text;
		}
		}
	}
}
