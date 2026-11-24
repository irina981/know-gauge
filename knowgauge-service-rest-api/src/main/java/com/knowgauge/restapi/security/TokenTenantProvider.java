package com.knowgauge.restapi.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentTenantProvider;

/** Security mode {@code oidc}: the tenant of the signed-in user. */
@Component
@ConditionalOnProperty(name = "kg.security.mode", havingValue = "oidc")
public class TokenTenantProvider implements CurrentTenantProvider {

	@Override
	public long tenantId() {
		return SignedInAccount.current().map(SignedInAccount::tenantId)
				.orElseThrow(() -> new IllegalStateException(
						"No tenant: the request is not signed in and no tenant was set with ExecutionContext.runAs"));
	}
}
