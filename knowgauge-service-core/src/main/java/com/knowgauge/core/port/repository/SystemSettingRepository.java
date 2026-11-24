package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.SystemSetting;

/** System-wide settings, one row per key. */
public interface SystemSettingRepository {

	List<SystemSetting> findAll();

	Optional<SystemSetting> findByKey(String key);

	SystemSetting save(SystemSetting setting);

	void delete(SystemSetting setting);

	/** Saves all rows in one transaction. */
	List<SystemSetting> saveAll(List<SystemSetting> settings);

	/** Removes every row (Reset to defaults). */
	void deleteAll();
}
