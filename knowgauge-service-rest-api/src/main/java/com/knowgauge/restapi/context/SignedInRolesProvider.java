package com.knowgauge.restapi.context;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentRolesProvider;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.restapi.security.SignedInAccount;

/**
 * The roles of the signed-in user of the current request (user_roles, ADR012). With sign-in off, and outside
 * requests, there is no account, so nothing is restricted by role (ADR017).
 */
@Component
public class SignedInRolesProvider implements CurrentRolesProvider {

	@Override
	public Optional<Set<Role>> currentRoles() {
		return SignedInAccount.current().map(account -> {
			Set<Role> roles = account.user().getRoles();
			return roles == null ? Set.<Role>of() : Set.copyOf(roles);
		});
	}
}
