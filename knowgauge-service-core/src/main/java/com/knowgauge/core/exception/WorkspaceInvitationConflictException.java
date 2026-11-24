package com.knowgauge.core.exception;

/**
 * A workspace invitation action conflicts with its state (ADR015): already accepted, the personal workspace already has
 * an owner or a pending invitation. Maps to HTTP 409.
 */
public class WorkspaceInvitationConflictException extends BaseException {

	private static final long serialVersionUID = 1L;

	public WorkspaceInvitationConflictException(String message) {
		super(message);
	}
}
