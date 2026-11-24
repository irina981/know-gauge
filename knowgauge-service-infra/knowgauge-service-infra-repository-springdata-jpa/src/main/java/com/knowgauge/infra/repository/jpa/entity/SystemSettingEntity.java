package com.knowgauge.infra.repository.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@Table(name = "system_settings")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SystemSettingEntity extends AuditableEntity {

	@Column(name = "setting_key", nullable = false, length = 100, unique = true)
	private String key;

	@Column(name = "setting_value", length = 1000)
	private String value;
}
