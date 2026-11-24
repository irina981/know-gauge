package com.knowgauge.core.service.settings;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One setting an {@code ADMIN} may change. Its default comes from the properties files (bound into a
 * {@code @ConfigurationProperties} bean); a {@code SYSTEM} row in {@code settings} under the same {@code key}
 * overrides it (System settings page), and a {@code TENANT} row overrides that for one tenant (ADR014). The code only
 * describes the setting: its type ({@code CONFIG} or {@code LIMIT}) and who may have a value of it ({@code SYSTEM},
 * {@code TENANT}) are decided by the database ({@code setting_configs}, {@code setting_config_owners}); every key
 * needs a row there (a Flyway migration), or startup fails. Only settings read on each use belong here, so a change
 * applies without a restart.
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
 */
public record SettingDefinition(String key, String group, String label, String description, Type type,
		List<String> options, Long min, Long max, boolean optional, Supplier<String> reader, Consumer<String> writer,
		Supplier<List<String>> suggestions) {

	/** A setting without suggestions. */
	public SettingDefinition(String key, String group, String label, String description, Type type,
			List<String> options, Long min, Long max, boolean optional, Supplier<String> reader,
			Consumer<String> writer) {
		this(key, group, label, description, type, options, min, max, optional, reader, writer, List::of);
	}

	public enum Type {
		INTEGER, TEXT, CHOICE
	}

	/** The values to offer; none when they cannot be had (e.g. the price catalog is unreachable). */
	public List<String> suggestionsOrNone() {
		try {
			return suggestions.get();
		} catch (RuntimeException e) {
			org.slf4j.LoggerFactory.getLogger(SettingDefinition.class)
					.warn("Suggestions for setting {} not available: {}", key, e.getMessage());
			return List.of();
		}
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
