package com.knowgauge.core.model.enums;

/**
 * Roles (ADR010). Only {@link #APPLICATION_ADMIN} and {@link #TENANT_ADMIN} are assigned for now;
 * {@link #TENANT_USER} and {@link #TENANT_VIEWER} are kept for organization tenants later.
 */
public enum Role {

    /**
     * Application admin, across tenants. A Keycloak realm role ({@code realm_access.roles} in the token), never
     * stored on a user row. The admin user that has it is created by the realm import.
     */
    APPLICATION_ADMIN,

    /** Manages everything in their own tenant. Every user has this role in their tenant (users.role). */
    TENANT_ADMIN,

    /** Runs tests in a tenant someone else administers. Not assigned yet. */
    TENANT_USER,

    /** Reads a tenant's content, tests and results, but changes nothing and generates nothing. Not assigned yet. */
    TENANT_VIEWER
}
