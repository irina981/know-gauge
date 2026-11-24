package com.knowgauge.core.model.enums;

/**
 * Roles, kept by KnowGauge itself (ADR012): table {@code roles} lists them, {@code user_roles} assigns them, and a
 * user can have several. The identity provider (Keycloak) only authenticates and knows nothing about them.
 * Every role can be assigned; tenant admins of an organization invite people as {@link #TENANT_ADMIN},
 * {@link #TENANT_USER} or {@link #TENANT_VIEWER} (ADR016). Only {@code /api/tenant/**} checks them so far.
 */
public enum Role {
    /**
     * Application admin, across tenants: changes the shared library, uses the admin API and actuator. Granted at
     * sign-in to the verified emails in {@code kg.account.admin-emails}.
     */
    ADMIN,
    /** Manages everything in their own tenant. Every user has this role in their tenant. */
    TENANT_ADMIN,
    /** Works with the library, tests and attempts of a workspace someone else administers (ADR016). */
    TENANT_USER,
    /** Reads a tenant's content, tests and results, but changes nothing and generates nothing (ADR016). */
    TENANT_VIEWER
}
