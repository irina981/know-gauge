package com.knowgauge.core.service.account;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.UserPreferences;

import lombok.extern.slf4j.Slf4j;

/**
 * Non-transactional so that a sign-in that loses the race to create the same
 * user (two first requests at once) can retry in a new transaction and find the
 * row the other one created.
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

	/** {@code external_id} of the system user row (V5 migration). */
	static final String SYSTEM_EXTERNAL_ID = "system";

	private static final int MAX_NAME_LENGTH = 200;
	private static final int MAX_EMAIL_LENGTH = 320;

	private final AccountTransactionalServiceImpl transactionalService;
	private final ExecutionContext executionContext;

	public AccountServiceImpl(AccountTransactionalServiceImpl transactionalService, ExecutionContext executionContext) {
		this.transactionalService = transactionalService;
		this.executionContext = executionContext;
	}

	@Override
	public User signIn(String externalId, String email, boolean emailVerified, String displayName) {
		if (externalId == null || externalId.isBlank()) {
			throw new IllegalArgumentException("The token has no subject.");
		}
		String subject = externalId.trim();
		if (SYSTEM_EXTERNAL_ID.equals(subject)) {
			// Reserved for the system user (ExecutionContext.SYSTEM_USER_ID); no token may sign in as it.
			throw new IllegalArgumentException("The token subject is reserved.");
		}
		String normalizedEmail = truncate(email, MAX_EMAIL_LENGTH);
		String name = truncate(displayName, MAX_NAME_LENGTH);
		// The app writes these rows itself (nobody is signed in yet), so the audit columns record the system user.
		try {
			return executionContext.callAsSystemWithoutTenant(
					() -> transactionalService.signIn(subject, normalizedEmail, emailVerified, name));
		} catch (DataIntegrityViolationException e) {
			log.info("Concurrent first sign-in of subject {}; reading the user the other request created", subject);
			return executionContext.callAsSystemWithoutTenant(
					() -> transactionalService.signIn(subject, normalizedEmail, emailVerified, name));
		}
	}

	@Override
	public Optional<Tenant> findTenant(Long tenantId) {
		return transactionalService.findTenant(tenantId);
	}

	@Override
	public UserPreferences getPreferences(Long userId) {
		return transactionalService.getPreferences(userId);
	}

	@Override
	public UserPreferences updatePreferences(Long userId, UserPreferences preferences) {
		if (preferences == null) {
			throw new IllegalArgumentException("Preferences are required.");
		}
		return transactionalService.updatePreferences(userId, preferences);
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim().replaceAll("\\s+", " ");
		return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
	}
}
