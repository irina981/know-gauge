package com.knowgauge.core.model.enums;

public enum TenantType {

    /** A user's own workspace, created on their first sign-in. */
    INDIVIDUAL,

    /** A company workspace shared by several users (later). */
    ORGANIZATION,

    /**
     * Shared sample content (topics and documents) that every user may read and generate tests from, into their
     * own tenant. Has no members; only an {@link Role#ADMIN} may change it (ADR010, ADR012).
     */
    LIBRARY
}
