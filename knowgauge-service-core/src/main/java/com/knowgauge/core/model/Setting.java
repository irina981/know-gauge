package com.knowgauge.core.model;

import com.knowgauge.core.model.enums.SettingOwner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One stored setting, as text under its key (a {@link SettingConfig} key, the property name; see
 * {@link com.knowgauge.core.service.settings.SettingDefinition} for the keys and their types). A
 * {@link SettingOwner#SYSTEM} row overrides the properties files; a {@link SettingOwner#TENANT} row overrides the
 * system value for one tenant (ADR014). A missing row means the value of the level above applies.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Setting extends AuditableObject {

	private SettingOwner owner;
	/** The tenant id of a {@link SettingOwner#TENANT} row; {@code null} for {@link SettingOwner#SYSTEM}. */
	private Long ownerId;
	private String key;
	private String value;

	public static Setting system(String key, String value) {
		return Setting.builder().owner(SettingOwner.SYSTEM).key(key).value(value).build();
	}

	public static Setting tenant(Long tenantId, String key, String value) {
		return Setting.builder().owner(SettingOwner.TENANT).ownerId(tenantId).key(key).value(value).build();
	}
}
