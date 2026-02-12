package com.knowgauge.core.exception;

public abstract class EmbeddingException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected EmbeddingException(String message, Throwable cause) {
		super(message, cause);
	}

	protected EmbeddingException(String message) {
		super(message);
	}
}


