package com.knowgauge.core.service.admin;

import java.util.List;

import com.knowgauge.core.model.TenantSummary;

/** Tenants for an {@code ADMIN}, who may read every tenant's content (ADR013). */
public interface TenantAdminService {

	/** Every tenant (user workspaces and libraries), with its owner and content counts, by name. */
	List<TenantSummary> listTenants();
}
