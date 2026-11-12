package com.knowgauge.core.exception;

/**
 * The selected documents yield too few chunks for the requested number of
 * questions ({@code kg.testgen.defaults.min-chunks-per-question}). Maps to HTTP
 * 422.
 */
public class InsufficientContextException extends BaseException {

	private static final long serialVersionUID = 1L;

	public InsufficientContextException(int availableChunks, int requiredChunks, int questionCount) {
		super("The selected documents have " + availableChunks + " chunks, but " + questionCount + " questions need at least "
				+ requiredChunks + ". Reduce the question count or add documents.");
	}
}
