package com.knowgauge.restapi.context;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.ApplicationAdminProvider;
import com.knowgauge.restapi.security.SignedInAccount;

/**
 * Whether the signed-in user of the current request has the {@code ADMIN} role (user_roles, ADR012). With sign-in
 * off there is no account, so nobody is an application admin.
 */
@Component
public class SignedInApplicationAdminProvider implements ApplicationAdminProvider {

	@Override
	public boolean isApplicationAdmin() {
		return SignedInAccount.current().map(SignedInAccount::applicationAdmin).orElse(false);
	}
}
