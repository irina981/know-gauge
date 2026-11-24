package com.knowgauge.core.exception;

/**
 * The signed-in user may not accept this workspace invitation (ADR015): it was sent to another email address, or
 * their email is not verified. Maps to HTTP 403.
 */
public class WorkspaceInvitationForbiddenException extends BaseException {

	private static final long serialVersionUID = 1L;

	public WorkspaceInvitationForbiddenException(String message) {
		super(message);
	}
}
