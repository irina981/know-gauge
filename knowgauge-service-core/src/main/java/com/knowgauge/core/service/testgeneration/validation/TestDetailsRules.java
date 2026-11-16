package com.knowgauge.core.service.testgeneration.validation;

/**
 * Rules for a test's editable details, shared by test generation
 * ({@link TestDraftValidator}) and editing a test before it is published.
 */
public final class TestDetailsRules {

	public static final int MAX_NAME_LENGTH = 200;
	public static final int MAX_DESCRIPTION_LENGTH = 2000;

	private TestDetailsRules() {
	}

	/**
	 * @return the trimmed name
	 * @throws IllegalArgumentException if the name is blank or too long
	 */
	public static String normalizeName(String name) {
		String trimmed = name == null ? "" : name.strip();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Test.name must not be blank.");
		}
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("Test.name must be at most " + MAX_NAME_LENGTH + " characters.");
		}
		return trimmed;
	}

	/**
	 * @return the trimmed description, or {@code null} when it is blank
	 * @throws IllegalArgumentException if the description is too long
	 */
	public static String normalizeDescription(String description) {
		String trimmed = description == null ? null : description.strip();
		if (trimmed != null && trimmed.length() > MAX_DESCRIPTION_LENGTH) {
			throw new IllegalArgumentException(
					"Test.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters.");
		}
		return trimmed == null || trimmed.isEmpty() ? null : trimmed;
	}
}
