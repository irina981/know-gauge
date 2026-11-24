package com.knowgauge.core.support;

import static org.mockito.Mockito.mock;

import java.util.Optional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.access.RolePermissions;

/** {@link RolePermissions} for unit tests. */
public final class Permissions {

	private Permissions() {
	}

	/** Nobody signed in, as in jobs and security mode fixed: every action is allowed. */
	public static RolePermissions unrestricted() {
		return new RolePermissions(Optional::empty, mock(ExecutionContext.class), mock(TestRepository.class));
	}
}
