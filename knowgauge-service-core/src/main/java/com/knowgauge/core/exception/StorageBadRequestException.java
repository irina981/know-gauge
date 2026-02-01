package com.knowgauge.core.exception;

// Used for: invalid object key, invalid bucket name, unsupported content type, malformed request, 
// client returning 400-range errors (except auth & not-found)
public class StorageBadRequestException extends StorageException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StorageBadRequestException(String message) {
		super(message);
	}

	public StorageBadRequestException(Throwable cause) {
		super("Invalid request sent to storage service", cause);
	}

	public StorageBadRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
