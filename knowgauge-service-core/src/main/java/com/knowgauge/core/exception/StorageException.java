package com.knowgauge.core.exception;

public abstract class StorageException extends BaseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}