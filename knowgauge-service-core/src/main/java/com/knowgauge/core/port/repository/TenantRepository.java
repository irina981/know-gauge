package com.knowgauge.core.port.repository;

import java.util.Optional;

import com.knowgauge.core.model.Tenant;

public interface TenantRepository {
	Tenant save(Tenant tenant);

	Optional<Tenant> findById(Long id);
}
