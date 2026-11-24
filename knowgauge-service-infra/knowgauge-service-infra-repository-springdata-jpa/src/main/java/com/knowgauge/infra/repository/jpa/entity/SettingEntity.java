package com.knowgauge.infra.repository.jpa.entity;

import com.knowgauge.core.model.enums.SettingType;

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

/** A row of {@code settings} (ADR014): a system-wide or a per-tenant value under a key. */
@Entity
@Getter
@Setter
@Table(name = "settings")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SettingEntity extends AuditableEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "setting_type", nullable = false, length = 20)
	private SettingType type;

	/** {@code null} for a {@link SettingType#SYSTEM} row. */
	@Column(name = "tenant_id")
	private Long tenantId;

	@Column(name = "setting_key", nullable = false, length = 100)
	private String key;

	@Column(name = "setting_value", length = 1000)
	private String value;
}
