package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.enums.TenantType;

public interface TenantRepository {
	Tenant save(Tenant tenant);

	Optional<Tenant> findById(Long id);

	/** @return ids of the tenants of the given type, ascending */
	List<Long> findIdsByType(TenantType type);

	/** Every tenant (user workspaces and libraries) with its owner and content counts, ordered by name. */
	List<TenantSummary> findTenantSummaries();
}
