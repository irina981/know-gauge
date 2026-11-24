package com.knowgauge.core.port.repository;

import java.util.List;

import com.knowgauge.core.model.SettingConfig;

/** The possible setting keys ({@code setting_configs}), seeded by Flyway and read only. */
public interface SettingConfigRepository {

	List<SettingConfig> findAll();
}
