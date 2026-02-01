package com.knowgauge.core.exception;

public class StorageUnexpectedException extends StorageException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StorageUnexpectedException(String message) {
        super(message);
    }

    public StorageUnexpectedException(Throwable cause) {
        super("Unexpected error occurred while interacting with storage service", cause);
    }

    public StorageUnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}