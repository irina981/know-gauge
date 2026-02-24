package com.knowgauge.restapi.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility for parsing enum values case-insensitively from string inputs.
 * Provides consistent error messages across the API.
 */
public final class EnumParser {

	private EnumParser() {
		// utility class
	}

	/**
	 * Parse a string to an enum value, case-insensitively.
	 * 
	 * @param <E>       the enum type
	 * @param enumClass the enum class
	 * @param value     the string value to parse (can be null)
	 * @return the parsed enum value, or null if value is null or blank
	 * @throws IllegalArgumentException if the value doesn't match any enum constant
	 */
	public static <E extends Enum<E>> E parse(Class<E> enumClass, String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String normalized = value.trim().toUpperCase();

		try {
			return Enum.valueOf(enumClass, normalized);
		} catch (IllegalArgumentException e) {
			String validValues = Arrays.stream(enumClass.getEnumConstants())
					.map(Enum::name)
					.collect(Collectors.joining(", "));

			throw new IllegalArgumentException(
					String.format("Invalid %s value: '%s'. Valid values are: %s",
							enumClass.getSimpleName(), value, validValues),
					e);
		}
	}

	/**
	 * Parse a string to an enum value, case-insensitively, with a default fallback.
	 * 
	 * @param <E>          the enum type
	 * @param enumClass    the enum class
	 * @param value        the string value to parse (can be null)
	 * @param defaultValue the default value to return if value is null/blank
	 * @return the parsed enum value, or defaultValue if value is null or blank
	 * @throws IllegalArgumentException if the value doesn't match any enum constant
	 */
	public static <E extends Enum<E>> E parseOrDefault(Class<E> enumClass, String value, E defaultValue) {
		E parsed = parse(enumClass, value);
		return parsed != null ? parsed : defaultValue;
	}
}
