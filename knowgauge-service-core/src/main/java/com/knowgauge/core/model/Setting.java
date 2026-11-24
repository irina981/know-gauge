package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.SettingType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One stored setting, as text under its key (the property name; see
 * {@link com.knowgauge.core.service.settings.SettingDefinition} for the keys and their types). A {@link SettingType#SYSTEM}
 * row overrides the properties files; a {@link SettingType#TENANT} row overrides the system value for one tenant
 * (ADR014). A missing row means the value of the level above applies.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Setting extends AuditableObject {

	private SettingType type;
	/** The tenant a {@link SettingType#TENANT} row applies to; {@code null} for {@link SettingType#SYSTEM}. */
	private Long tenantId;
	private String key;
	private String value;

	public static Setting system(String key, String value) {
		return Setting.builder().type(SettingType.SYSTEM).key(key).value(value).build();
	}

	public static Setting tenant(Long tenantId, String key, String value) {
		return Setting.builder().type(SettingType.TENANT).tenantId(tenantId).key(key).value(value).build();
	}
}
