package com.knowgauge.restapi.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentTenantProvider;

/**
 * Security mode {@code fixed} (no sign-in): one tenant for the whole deployment, from
 * {@code kg.context.tenant-id} (env {@code KG_TENANT_ID} in prod).
 */
@Component
@ConditionalOnProperty(name = "kg.security.mode", havingValue = "fixed", matchIfMissing = true)
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
