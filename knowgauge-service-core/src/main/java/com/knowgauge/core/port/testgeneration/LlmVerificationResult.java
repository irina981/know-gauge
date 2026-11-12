package com.knowgauge.core.port.testgeneration;

/**
 * Outcome of one verification call.
 *
 * @param flaggedCount number of options flagged (0 when the call failed)
 * @param call         call metadata, null when the call failed before a
 *                     response
 * @param errorMessage null on success. Verification is best effort and never
 *                     fails generation
 */
public record LlmVerificationResult(int flaggedCount, LlmCallMetadata call, String errorMessage) {

	public boolean succeeded() {
		return errorMessage == null;
	}
}
