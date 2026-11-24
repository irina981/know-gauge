package com.knowgauge.core.model.enums;

/**
 * What a setting is for (column {@code setting_configs.setting_type}). The System settings page shows each type on a
 * tab of its own.
 */
public enum SettingType {

    /** A configuration value, e.g. the default difficulty or number of questions of a test, or an AI model. */
    CONFIG,

    /** A limit on what a workspace or user may use, e.g. storage, AI tokens per month or concurrent generations. */
    LIMIT
}
