package com.knowgauge.core.service.account;

import java.util.Optional;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.UserPreferences;

/**
 * Local users and tenants of the people who sign in through the identity
 * provider. The provider authenticates; this service maps its subject to a user
 * and the user's tenant.
 */
public interface AccountService {

	/**
	 * Returns the user with this subject, creating the user and a personal tenant
	 * of their own on the first sign-in. Updates the email and display name when
	 * the provider reports new ones.
	 *
	 * @param externalId  the provider's subject ({@code sub}), required
	 * @param email       optional
	 * @param displayName optional
	 */
	User signIn(String externalId, String email, String displayName);

	Optional<Tenant> findTenant(Long tenantId);

	/**
	 * @return the user's settings; the defaults (nothing chosen) when they have none or have no user row (sign-in
	 *         off)
	 */
	UserPreferences getPreferences(Long userId);

	/**
	 * Replaces the user's settings.
	 *
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException when the user has no user row (sign-in off
	 *                                                                 without a stored user)
	 */
	UserPreferences updatePreferences(Long userId, UserPreferences preferences);
}
