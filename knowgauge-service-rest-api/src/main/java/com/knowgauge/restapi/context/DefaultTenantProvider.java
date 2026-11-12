package com.knowgauge.restapi.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentTenantProvider;

/**
 * Fixed tenant from {@code kg.context.tenant-id} (env {@code KG_TENANT_ID} in
 * prod). Stands in until authentication provides the tenant.
 */
@Component
public class DefaultTenantProvider implements CurrentTenantProvider {

	private final long tenantId;

	public DefaultTenantProvider(@Value("${kg.context.tenant-id}") long tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public long tenantId() {
		return tenantId;
	}
}
