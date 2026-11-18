package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A person who signs in through the identity provider (Keycloak). The local row
 * links the provider's subject to a tenant; the provider owns the password,
 * email verification and roles.
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

	/** Role in the tenant: {@link Role#TENANT_ADMIN} for every user for now. */
	private Role role;

	/** Email from the identity provider at the last sign-in. */
	private String email;

	private String displayName;

	/** When the service last resolved the user from a token (not on every request). */
	private Instant lastSeenAt;
}
