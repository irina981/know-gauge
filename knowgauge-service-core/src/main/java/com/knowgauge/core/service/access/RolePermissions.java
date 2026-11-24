package com.knowgauge.core.service.access;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentRolesProvider;
import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.PermissionDeniedException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.port.repository.TestRepository;

/**
 * What the signed-in user's role allows in their workspace (ADR017):
 * <ul>
 * <li>{@code TENANT_ADMIN} (and {@code ADMIN}): everything.</li>
 * <li>{@code TENANT_USER}: generates tests, changes only the tests they created, takes tests. Never the library.</li>
 * <li>{@code TENANT_VIEWER}: reads only.</li>
 * </ul>
 * Work without a signed-in user (jobs, startup seeding, security mode {@code fixed}) is not restricted. Reading is
 * never checked here, and these checks come on top of the tenant and shared-library rules ({@code ContentScope}).
 */
@Component
public class RolePermissions {

	private final CurrentRolesProvider rolesProvider;
	private final ExecutionContext executionContext;
	private final TestRepository testRepository;

	public RolePermissions(CurrentRolesProvider rolesProvider, ExecutionContext executionContext,
			TestRepository testRepository) {
		this.rolesProvider = rolesProvider;
		this.executionContext = executionContext;
		this.testRepository = testRepository;
	}

	/** Whether the user may change the library: topics and documents (tenant admins only). */
	public boolean canChangeLibrary() {
		return mayChangeLibrary(rolesProvider.currentRoles());
	}

	/** The library rule for the given roles; empty roles (nobody signed in) are not restricted. */
	public static boolean mayChangeLibrary(Optional<Set<Role>> roles) {
		return roles.map(RolePermissions::isAdmin).orElse(true);
	}

	/** @throws PermissionDeniedException unless the user may change the library */
	public void requireChangeLibrary() {
		if (!canChangeLibrary()) {
			throw new PermissionDeniedException("Only an admin of this workspace can change its library.");
		}
	}

	/** @throws PermissionDeniedException for a viewer, who generates no tests */
	public void requireCreateTests() {
		if (!canCreateTests()) {
			throw new PermissionDeniedException("A viewer cannot generate tests.");
		}
	}

	/** @throws PermissionDeniedException for a viewer, who takes no tests in the app */
	public void requireTakeTests() {
		if (!canCreateTests()) {
			throw new PermissionDeniedException("A viewer cannot take tests.");
		}
	}

	/** Whether the user may generate and take tests: tenant admins and users. */
	public boolean canCreateTests() {
		return rolesProvider.currentRoles().map(r -> isAdmin(r) || r.contains(Role.TENANT_USER)).orElse(true);
	}

	/** Whether the user may change the test: any test for a tenant admin, their own for a user. */
	public boolean canChangeTest(Test test) {
		Optional<Set<Role>> roles = rolesProvider.currentRoles();
		if (roles.isEmpty() || isAdmin(roles.get())) {
			return true;
		}
		if (!roles.get().contains(Role.TENANT_USER) || test == null) {
			return false;
		}
		OptionalLong userId = executionContext.userId();
		return userId.isPresent() && Objects.equals(test.getCreatedBy(), userId.getAsLong());
	}

	/**
	 * Checks the test of the current tenant; an unknown test passes, so the service reports it as not found.
	 *
	 * @throws PermissionDeniedException unless the user may change the test
	 */
	public void requireChangeTest(Long testId) {
		Optional<Set<Role>> roles = rolesProvider.currentRoles();
		if (roles.isEmpty() || isAdmin(roles.get())) {
			return;
		}
		Optional<Test> test = testRepository.findByTenantIdAndId(executionContext.tenantId(), testId);
		if (test.isPresent() && !canChangeTest(test.get())) {
			throw new PermissionDeniedException(roles.get().contains(Role.TENANT_USER)
					? "You can change only the tests you created. Ask an admin of this workspace."
					: "A viewer cannot change tests.");
		}
	}

	private static boolean isAdmin(Set<Role> roles) {
		return roles.contains(Role.TENANT_ADMIN) || roles.contains(Role.ADMIN);
	}
}
