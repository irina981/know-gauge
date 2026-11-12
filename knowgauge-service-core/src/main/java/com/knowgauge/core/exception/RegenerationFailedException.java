package com.knowgauge.core.exception;

/**
 * The LLM could not produce a valid replacement question. Nothing was changed.
 * Maps to HTTP 502.
 */
public class RegenerationFailedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public RegenerationFailedException(String message) {
		super(message);
	}

	public RegenerationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
