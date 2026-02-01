package com.knowgauge.core.exception;

// Used for transient failures: network issues, timeouts, 5xx
public class StorageUnavailableException extends StorageException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(Throwable cause) {
        super("Storage service is temporarily unavailable", cause);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}