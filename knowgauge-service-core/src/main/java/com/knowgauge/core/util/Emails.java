package com.knowgauge.core.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email addresses of participants and invitees (ADR009): trimmed and
 * lower-cased, so the same person is recognised however they type it.
 */
public final class Emails {

	/** Deliberately simple: something@something.tld, no spaces. The mail server has the final word. */
	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	private static final int MAX_LENGTH = 320;

	private Emails() {
	}

	/** @return the normalized address, or {@code null} if it is not a plausible email */
	public static String normalizeOrNull(String email) {
		String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		return !normalized.isEmpty() && normalized.length() <= MAX_LENGTH && EMAIL.matcher(normalized).matches()
				? normalized
				: null;
	}

	/** @throws IllegalArgumentException if the address is missing or not plausible */
	public static String normalize(String email) {
		String normalized = normalizeOrNull(email);
		if (normalized == null) {
			throw new IllegalArgumentException("Please enter a valid email address.");
		}
		return normalized;
	}
}
