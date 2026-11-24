package com.knowgauge.infra.repository.jpa.entity;

import java.util.EnumSet;
import java.util.Set;

import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.model.enums.SettingType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A row of {@code setting_configs}: one possible setting key, its type and (from {@code setting_config_owners}) who
 * may have a value of it. Seeded by Flyway, read only.
 */
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

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "setting_config_owners", joinColumns = @JoinColumn(name = "setting_key"))
	@Enumerated(EnumType.STRING)
	@Column(name = "setting_owner", nullable = false, length = 20)
	private Set<SettingOwner> owners = EnumSet.noneOf(SettingOwner.class);
}
