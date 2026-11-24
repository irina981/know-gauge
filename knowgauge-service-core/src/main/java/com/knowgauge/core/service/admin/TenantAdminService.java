package com.knowgauge.core.service.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.TenantSummary;

/** Tenants for an {@code ADMIN}, who may read every tenant's content (ADR013). */
public interface TenantAdminService {

	/**
	 * Tenants (user workspaces and libraries) with their owner and content counts, filtered, sorted and paged.
	 *
	 * @throws IllegalArgumentException for an unknown sort property
	 */
	Page<TenantSummary> listTenants(TenantSearch search, Pageable pageable);

	/** @throws com.knowgauge.core.exception.ResourceNotFoundException when there is no such tenant */
	TenantSummary getTenant(Long tenantId);
}
