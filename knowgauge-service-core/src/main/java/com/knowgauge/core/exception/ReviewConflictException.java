package com.knowgauge.core.exception;

/**
 * A review action conflicts with the current state: the question was changed by
 * someone else (stale expected version), the test is not in a reviewable status,
 * or the action does not apply to the question. Maps to HTTP 409.
 */
public class ReviewConflictException extends BaseException {

	private static final long serialVersionUID = 1L;

	public ReviewConflictException(String message) {
		super(message);
	}
}
