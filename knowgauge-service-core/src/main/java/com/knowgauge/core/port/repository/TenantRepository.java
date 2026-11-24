package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.enums.TenantType;

public interface TenantRepository {
	Tenant save(Tenant tenant);

	Optional<Tenant> findById(Long id);

	/** @return ids of the tenants of the given type, ascending */
	List<Long> findIdsByType(TenantType type);

	/**
	 * Tenants (user workspaces and libraries) with their owner and content counts, filtered and paged. Sort properties:
	 * {@code name}, {@code type}, {@code owner}, {@code topics}, {@code documents}, {@code createdAt}.
	 *
	 * @throws IllegalArgumentException for any other sort property
	 */
	Page<TenantSummary> findTenantSummaries(TenantSearch search, Pageable pageable);

	/** One tenant with its owner and content counts. */
	Optional<TenantSummary> findTenantSummary(Long tenantId);
}
