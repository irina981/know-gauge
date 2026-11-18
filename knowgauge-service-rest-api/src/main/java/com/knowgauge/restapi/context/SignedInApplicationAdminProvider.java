package com.knowgauge.restapi.context;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.ApplicationAdminProvider;
import com.knowgauge.restapi.security.SignedInAccount;

/**
 * Reads the {@code APPLICATION_ADMIN} realm role from the signed-in account of the current request. With sign-in
 * off there is no account, so nobody is an application admin.
 */
@Component
public class SignedInApplicationAdminProvider implements ApplicationAdminProvider {

	@Override
	public boolean isApplicationAdmin() {
		return SignedInAccount.current().map(SignedInAccount::applicationAdmin).orElse(false);
	}
}
