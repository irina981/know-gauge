package com.knowgauge.core.exception;

/**
 * An attempt action conflicts with the current state: the test is not published
 * or has no questions, or the attempt was already submitted. Maps to HTTP 409.
 */
public class AttemptConflictException extends BaseException {

	private static final long serialVersionUID = 1L;

	public AttemptConflictException(String message) {
		super(message);
	}
}
