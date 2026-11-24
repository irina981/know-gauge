package com.knowgauge.core.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.knowgauge.core.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A person who signs in through the identity provider (Keycloak). The local row
 * links the provider's subject to a tenant; the provider owns the password and
 * email verification. KnowGauge owns the roles (ADR012).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends AuditableObject {

	/** Subject ({@code sub} claim) of the identity provider; unique. */
	private String externalId;

	/** The one tenant the user belongs to. */
	private Long tenantId;

	/**
	 * The user's roles (table {@code user_roles}, ADR012): {@link Role#TENANT_ADMIN} of their tenant for every user,
	 * plus {@link Role#ADMIN} for application admins. Kept by KnowGauge, never taken from the identity provider.
	 */
	@Builder.Default
	private Set<Role> roles = EnumSet.noneOf(Role.class);

	/** Email from the identity provider at the last sign-in. */
	private String email;

	private String displayName;

	/** When the service last resolved the user from a token (not on every request). */
	private Instant lastSeenAt;

	/** Settings the user chose for the app; {@code null} until they change one. */
	private UserPreferences preferences;

	public boolean hasRole(Role role) {
		return roles != null && roles.contains(role);
	}
}
