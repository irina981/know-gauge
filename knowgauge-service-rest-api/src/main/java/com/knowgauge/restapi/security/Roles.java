package com.knowgauge.restapi.security;

import com.knowgauge.core.model.enums.Role;

/**
 * Role names for {@code @RolesAllowed} on controller methods (ADR017), as compile-time constants: an annotation
 * cannot take {@code Role.X.name()}. Each equals a {@link Role} constant; {@code RolesTest} keeps them in step.
 */
public final class Roles {

	/** {@link Role#ADMIN}: the application admin. */
	public static final String ADMIN = "ADMIN";
	/** {@link Role#TENANT_ADMIN}: everything in the workspace. */
	public static final String TENANT_ADMIN = "TENANT_ADMIN";
	/** {@link Role#TENANT_USER}: creates tests and changes their own; takes tests. */
	public static final String TENANT_USER = "TENANT_USER";
	/** {@link Role#TENANT_VIEWER}: reads only, so it is never in an {@code @RolesAllowed} list. */
	public static final String TENANT_VIEWER = "TENANT_VIEWER";

	private Roles() {
	}
}
