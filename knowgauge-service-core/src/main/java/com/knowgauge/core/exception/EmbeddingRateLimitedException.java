package com.knowgauge.core.exception;

public final class EmbeddingRateLimitedException extends EmbeddingException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EmbeddingRateLimitedException(String message, Throwable cause) {
		super(message, cause);
	}
}