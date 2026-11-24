package com.knowgauge.restapi.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.oauth2.jwt.Jwt;

import com.knowgauge.core.model.User;
import com.knowgauge.core.service.account.AccountService;

/**
 * Maps a token to the local user (and so the tenant), creating both on the first sign-in. Results are cached per
 * subject for {@code kg.security.oidc.account-cache-minutes}, so most requests don't touch the database.
 */
public class AccountResolver {

	/** Bounds memory; the cache is simply emptied when it gets this big. */
	private static final int MAX_CACHED = 10_000;

	private record Cached(User user, Instant loadedAt) {
	}

	private final AccountService accountService;
	private final Duration refreshAfter;
	private final Map<String, Cached> cache = new ConcurrentHashMap<>();

	public AccountResolver(AccountService accountService, Duration refreshAfter) {
		this.accountService = accountService;
		this.refreshAfter = refreshAfter;
	}

	public User resolve(Jwt jwt) {
		String subject = jwt.getSubject();
		Instant now = Instant.now();
		Cached cached = subject == null ? null : cache.get(subject);
		if (cached != null && cached.loadedAt().plus(refreshAfter).isAfter(now)) {
			return cached.user();
		}
		User user = accountService.signIn(subject, jwt.getClaimAsString("email"),
				Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")), displayName(jwt));
		if (cache.size() >= MAX_CACHED) {
			cache.clear();
		}
		cache.put(subject, new Cached(user, now));
		return user;
	}

	/** Forgets the cached user of {@code subject}, e.g. after they moved to another workspace (ADR015). */
	public void evict(String subject) {
		if (subject != null) {
			cache.remove(subject);
		}
	}

	private static String displayName(Jwt jwt) {
		String name = jwt.getClaimAsString("name");
		return name != null && !name.isBlank() ? name : jwt.getClaimAsString("preferred_username");
	}
}
