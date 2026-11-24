package com.knowgauge.infra.repository.jpa.entity;

import com.knowgauge.core.model.enums.SettingOwner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A row of {@code settings} (ADR014): a value under a key of {@code setting_configs}, for the whole system or for one
 * owner (a tenant).
 */
@Entity
@Getter
@Setter
@Table(name = "settings")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SettingEntity extends AuditableEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "setting_owner", nullable = false, length = 20)
	private SettingOwner owner;

	/** The tenant id of a {@link SettingOwner#TENANT} row; {@code null} for {@link SettingOwner#SYSTEM}. */
	@Column(name = "owner_id")
	private Long ownerId;

	/** References {@code setting_configs.setting_key}. */
	@Column(name = "setting_key", nullable = false, length = 100)
	private String key;

	@Column(name = "setting_value", length = 1000)
	private String value;
}
