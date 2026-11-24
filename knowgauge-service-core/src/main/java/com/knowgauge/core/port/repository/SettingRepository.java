package com.knowgauge.core.port.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Setting;

/** Stored settings (ADR014): system rows, one per key, and tenant rows, one per tenant and key. */
public interface SettingRepository {

	/** @return every {@code SYSTEM} row */
	List<Setting> findSystem();

	Optional<Setting> findSystem(String key);

	/** @return the {@code TENANT} rows of one tenant */
	List<Setting> findByTenantId(Long tenantId);

	/** @return the {@code TENANT} rows of the given tenants */
	List<Setting> findByTenantIdIn(Collection<Long> tenantIds);

	/** Saves all rows in one transaction. */
	List<Setting> saveAll(List<Setting> settings);

	/** Removes the given rows in one transaction. */
	void deleteAll(List<Setting> settings);

	/** Removes every {@code SYSTEM} row (Reset to defaults on the System settings page). */
	void deleteSystem();

	/** Removes every {@code TENANT} row of one tenant, so the system values apply to it again. */
	void deleteByTenantId(Long tenantId);
}
