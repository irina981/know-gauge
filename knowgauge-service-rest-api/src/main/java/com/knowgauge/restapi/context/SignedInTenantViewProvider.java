package com.knowgauge.restapi.context;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.TenantViewProvider;
import com.knowgauge.restapi.security.SignedInAccount;

/**
 * Whether the current request is an {@code ADMIN} reading another user's library, a tenant (ADR013). Never with sign-in
 * off, since there is no account then.
 */
@Component
public class SignedInTenantViewProvider implements TenantViewProvider {

	@Override
	public boolean isViewingOtherTenant() {
		return SignedInAccount.current().map(account -> account.viewedTenantId() != null).orElse(false);
	}
}
