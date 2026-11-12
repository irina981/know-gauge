package com.knowgauge.core.port.testgeneration;

/**
 * What an LLM call actually did, recorded in {@code generation_runs}.
 *
 * @param model        model name used for the call
 * @param rawResponse  raw text returned by the model
 * @param inputTokens  prompt tokens, if reported
 * @param outputTokens completion tokens, if reported
 * @param finishReason provider finish reason, if reported
 */
public record LlmCallMetadata(String model, String rawResponse, Integer inputTokens, Integer outputTokens,
		String finishReason) {
}
