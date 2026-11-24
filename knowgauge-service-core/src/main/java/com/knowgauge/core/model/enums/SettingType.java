package com.knowgauge.core.model.enums;

/** The level a stored setting applies to (ADR014). A more specific level overrides a general one. */
public enum SettingType {

    /** Applies everywhere, overriding the properties files. Kept on the System settings page. */
    SYSTEM,

    /**
     * Applies to one tenant (workspace), overriding the system value. Only settings marked as workspace-level may
     * have one, e.g. the workspace limits.
     */
    TENANT
}
