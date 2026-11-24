package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.TenantType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Owner of all content (topics, documents, tests, attempts): every table has a
 * {@code tenant_id}. A user gets a {@link TenantType#PERSONAL} tenant of their
 * own on first sign-in, unless an invitation is waiting for them.
 * {@link TenantType#LIBRARY} tenants hold shared content everyone may read.
 * An ADMIN also creates tenants of any type and invites people to them by email
 * (ADR015); an {@link TenantType#ORGANIZATION} may have several users.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tenant extends AuditableObject {

	private TenantType type;

	/** Shown to the user, e.g. their name for a personal tenant, or the company name. */
	private String name;

	/**
	 * The user the tenant was created for: the first signer-in of a personal tenant, or the invitee who accepted a
	 * personal one an ADMIN created. {@code null} for libraries, organizations and tenants created by migrations.
	 */
	private Long ownerUserId;
}
