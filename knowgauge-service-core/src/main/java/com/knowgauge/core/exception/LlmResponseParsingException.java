package com.knowgauge.core.exception;

/**
 * Exception thrown when LLM response cannot be parsed or when response
 * generation was terminated due to constraints (e.g., max tokens).
 */
public class LlmResponseParsingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public enum Reason {
		/**
		 * Response generation stopped because max output tokens was reached.
		 */
		LENGTH,

		/**
		 * Response JSON could not be parsed.
		 */
		PARSING_ERROR,
		
		/**
		 * Response JSON could not be mapped.
		 */
		MAPPING_ERROR,

		/**
		 * Response was empty or invalid.
		 */
		INVALID_RESPONSE,

		/**
		 * Other unspecified error.
		 */
		OTHER
	}

	private final Reason reason;

	public LlmResponseParsingException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public LlmResponseParsingException(Reason reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
