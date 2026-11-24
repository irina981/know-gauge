package com.knowgauge.core.exception;

/** The user already generates as many tests at once as allowed (system setting). */
public class GenerationLimitReachedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public GenerationLimitReachedException(String message) {
		super(message);
	}
}
