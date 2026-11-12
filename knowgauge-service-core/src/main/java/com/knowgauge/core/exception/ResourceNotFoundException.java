package com.knowgauge.core.exception;

/**
 * A requested resource does not exist for the current tenant. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends BaseException {

	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
