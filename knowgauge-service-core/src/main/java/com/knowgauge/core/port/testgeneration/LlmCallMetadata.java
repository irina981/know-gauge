package com.knowgauge.core.port.testgeneration;

import java.util.Map;

/**
 * What an LLM call actually sent and returned, recorded in
 * {@code generation_runs}.
 *
 * @param model         model name used for the call
 * @param prompt        full prompt text sent, including any suffix the adapter
 *                      appends
 * @param requestParams request settings that shaped the call (temperature, max
 *                      output tokens, response format, JSON schema)
 * @param rawResponse   raw text returned by the model
 * @param inputTokens   prompt tokens, if reported
 * @param outputTokens  completion tokens, if reported
 * @param finishReason  provider finish reason, if reported
 */
public record LlmCallMetadata(String model, String prompt, Map<String, Object> requestParams, String rawResponse,
		Integer inputTokens, Integer outputTokens, String finishReason) {

	/** The request side only, for a call that produced no usable response. */
	public static LlmCallMetadata requestOnly(String model, String prompt, Map<String, Object> requestParams) {
		return new LlmCallMetadata(model, prompt, requestParams, null, null, null, null);
	}

	/** Copy with the request side set, keeping the response side. */
	public LlmCallMetadata withRequest(String prompt, Map<String, Object> requestParams) {
		return new LlmCallMetadata(model, prompt, requestParams, rawResponse, inputTokens, outputTokens, finishReason);
	}
}
