package com.knowgauge.core.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Random, URL-safe tokens for public links (ADR009): 24 random bytes, 32
 * characters. Unguessable, so a link to one test or attempt does not reveal
 * others.
 */
public final class PublicTokens {

	private static final SecureRandom RANDOM = new SecureRandom();

	private PublicTokens() {
	}

	public static String newToken() {
		byte[] bytes = new byte[24];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
