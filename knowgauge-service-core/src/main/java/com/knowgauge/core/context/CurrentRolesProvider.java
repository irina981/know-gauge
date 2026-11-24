package com.knowgauge.core.context;

import java.util.Optional;
import java.util.Set;

import com.knowgauge.core.model.enums.Role;

/**
 * The roles of the user the current request runs for (user_roles, ADR012), which decide what they may do in their
 * workspace (ADR017).
 */
public interface CurrentRolesProvider {

	/**
	 * @return the signed-in user's roles; empty when nobody is signed in: work outside a request (jobs, startup
	 *         seeding) and security mode {@code fixed}, which are not restricted by role
	 */
	Optional<Set<Role>> currentRoles();
}
