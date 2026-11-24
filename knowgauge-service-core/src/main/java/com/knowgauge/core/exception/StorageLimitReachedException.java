package com.knowgauge.core.exception;

/** An upload would take the workspace over its storage limit (system setting). */
public class StorageLimitReachedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public StorageLimitReachedException(String message) {
		super(message);
	}
}
