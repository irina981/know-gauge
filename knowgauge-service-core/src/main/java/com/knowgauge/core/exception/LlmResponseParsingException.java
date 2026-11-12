package com.knowgauge.core.exception;

import com.knowgauge.core.port.testgeneration.LlmCallMetadata;

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

	/** What was sent (and returned, if anything), so the failed call can be recorded. */
	private transient LlmCallMetadata call;

	public Reason getReason() {
		return reason;
	}

	public LlmCallMetadata getCall() {
		return call;
	}

	/** Attaches (or replaces) the call metadata. */
	public LlmResponseParsingException withCall(LlmCallMetadata call) {
		this.call = call;
		return this;
	}
}
