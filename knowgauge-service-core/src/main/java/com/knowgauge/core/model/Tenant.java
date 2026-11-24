package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.TenantType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Owner of all content (topics, documents, tests, attempts): every table has a
 * {@code tenant_id}. For now each user gets an {@link TenantType#PERSONAL}
 * tenant of their own on first sign-in. {@link TenantType#LIBRARY} tenants
 * hold shared sample content everyone may read. {@link TenantType#ORGANIZATION}
 * tenants shared by several users come later.
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

	/** The user the tenant was created for; {@code null} for tenants created by migrations. */
	private Long ownerUserId;
}
