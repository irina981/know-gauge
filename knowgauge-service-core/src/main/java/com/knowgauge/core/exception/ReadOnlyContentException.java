package com.knowgauge.core.exception;

/**
 * The content exists and may be read, but the current tenant may not change it: it belongs to the library
 * (ADR010). Maps to HTTP 403.
 */
public class ReadOnlyContentException extends BaseException {

	private static final long serialVersionUID = 1L;

	public ReadOnlyContentException(String message) {
		super(message);
	}
}
