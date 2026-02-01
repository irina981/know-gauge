package com.knowgauge.core.exception;

// Used for: wrong credentials, policy denied, 401/403
public class StorageAuthException extends StorageException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StorageAuthException(String message) {
        super(message);
    }

    public StorageAuthException(Throwable cause) {
        super("Authentication/authorization with storage failed", cause);
    }

    public StorageAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
