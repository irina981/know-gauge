package com.knowgauge.core.service.workspace;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.knowgauge.core.util.Emails;

/** Checks the addresses and the note an admin enters for workspace invitations. */
final class InvitationInput {

	static final int MAX_MESSAGE_LENGTH = 2000;

	private InvitationInput() {
	}

	/**
	 * @return the addresses lower-cased, without blanks and duplicates, in the order given (may be empty)
	 * @throws IllegalArgumentException for an invalid address or more than {@code max}
	 */
	static List<String> emails(List<String> emails, int max) {
		Set<String> addresses = new LinkedHashSet<>();
		List<String> invalid = new ArrayList<>();
		if (emails != null) {
			for (String email : emails) {
				if (email == null || email.isBlank()) {
					continue;
				}
				String normalized = Emails.normalizeOrNull(email);
				if (normalized == null) {
					invalid.add(email.trim());
				} else {
					addresses.add(normalized);
				}
			}
		}
		if (!invalid.isEmpty()) {
			throw new IllegalArgumentException("These are not valid email addresses: " + String.join(", ", invalid));
		}
		if (addresses.size() > max) {
			throw new IllegalArgumentException(
					"At most " + max + " addresses can be invited at once, but " + addresses.size() + " were given.");
		}
		return List.copyOf(addresses);
	}

	/** @return the trimmed note, {@code null} when blank */
	static String message(String message) {
		if (message == null || message.isBlank()) {
			return null;
		}
		String trimmed = message.trim();
		if (trimmed.length() > MAX_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("The message can be at most " + MAX_MESSAGE_LENGTH + " characters.");
		}
		return trimmed;
	}
}
