package com.knowgauge.infra.repository.jpa.entity;

import com.knowgauge.core.model.enums.SettingType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A row of {@code setting_configs}: one possible setting key and its type. Seeded by Flyway, read only. */
@Entity
@Getter
@Setter
@Table(name = "setting_configs")
@NoArgsConstructor
@AllArgsConstructor
public class SettingConfigEntity {

	@Id
	@Column(name = "setting_key", nullable = false, length = 100)
	private String key;

	@Enumerated(EnumType.STRING)
	@Column(name = "setting_type", nullable = false, length = 20)
	private SettingType type;
}
