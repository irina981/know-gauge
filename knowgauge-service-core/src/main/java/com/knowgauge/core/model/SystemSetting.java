package com.knowgauge.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One system-wide setting kept by an {@code ADMIN} on the System settings page, as text under a key (see
 * {@link com.knowgauge.core.service.settings.SystemSettingsService} for the keys and their types). A missing row means
 * the default.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SystemSetting extends AuditableObject {
	private String key;
	private String value;
}
