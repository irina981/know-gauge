package com.knowgauge.core.model.enums;

/**
 * Roles, kept by KnowGauge itself (ADR012): table {@code roles} lists them, {@code user_roles} assigns them, and a
 * user can have several. The identity provider (Keycloak) only authenticates and knows nothing about them.
 * Only {@link #ADMIN} and {@link #TENANT_ADMIN} are assigned for now; {@link #TENANT_USER} and
 * {@link #TENANT_VIEWER} are kept for organization tenants later.
 */
public enum Role {
    /**
     * Application admin, across tenants: changes the shared library, uses the admin API and actuator. Granted at
     * sign-in to the verified emails in {@code kg.account.admin-emails}.
     */
    ADMIN,
    /** Manages everything in their own tenant. Every user has this role in their tenant. */
    TENANT_ADMIN,
    /** Runs tests in a tenant someone else administers. Not assigned yet. */
    TENANT_USER,
    /** Reads a tenant's content, tests and results, but changes nothing and generates nothing. Not assigned yet. */
    TENANT_VIEWER
}
