package com.knowgauge.core.exception;

/**
 * The signed-in user's role in their workspace does not allow the action (ADR017), e.g. a {@code TENANT_VIEWER}
 * generating a test or a {@code TENANT_USER} changing a colleague's test. Maps to HTTP 403.
 */
public class PermissionDeniedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public PermissionDeniedException(String message) {
		super(message);
	}
}
