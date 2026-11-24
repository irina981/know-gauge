package com.knowgauge.core.exception;

/**
 * The current user may not manage their workspace's users or settings: it is not an organization workspace, the
 * setting is a limit (an ADMIN sets those), or the request only views another user's workspace (ADR013, ADR016).
 * Maps to HTTP 403.
 */
public class TenantAccessDeniedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public TenantAccessDeniedException(String message) {
		super(message);
	}
}
