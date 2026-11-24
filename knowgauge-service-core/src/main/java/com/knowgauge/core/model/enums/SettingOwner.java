package com.knowgauge.core.model.enums;

/**
 * Who a stored setting belongs to (ADR014): the level it applies to. A more specific owner overrides a general one.
 * More owners may follow (e.g. a billing plan), each with its own kind of {@code owner_id}.
 */
public enum SettingOwner {

    /** Applies everywhere, overriding the properties files. Has no owner id. Kept on the System settings page. */
    SYSTEM,

    /**
     * Applies to one tenant (workspace; the owner id is the tenant id), overriding the system value. Only settings
     * marked as tenant-level may have one, e.g. the tenant limits.
     */
    TENANT
}
