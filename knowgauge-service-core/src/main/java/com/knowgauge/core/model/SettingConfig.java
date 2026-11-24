package com.knowgauge.core.model;

import java.util.Set;

import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.model.enums.SettingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One possible setting key, a row of {@code setting_configs} with its owners from {@code setting_config_owners}
 * (seeded by Flyway). The database decides a setting's type and who may have a value of it; the code only describes
 * the key ({@link com.knowgauge.core.service.settings.SettingDefinition}). Every stored {@link Setting} references
 * one of the key's owners.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingConfig {

	private String key;
	private SettingType type;
	/** Who may have a value: {@link SettingOwner#SYSTEM} (System settings page), {@link SettingOwner#TENANT}, or both. */
	private Set<SettingOwner> owners;

	public boolean allows(SettingOwner owner) {
		return owners != null && owners.contains(owner);
	}
}
