package com.knowgauge.infra.testgeneration.langchain4j.openai.mapper;

import java.util.Map;

import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.port.testgeneration.LlmCallMetadata;

import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Builds what the LLM call sent (prompt, request settings) and returned
 * (model, raw text, token usage, finish reason) for the generation run record.
 */
public final class LlmCallMetadataMapper {

	private LlmCallMetadataMapper() {
	}

	/** Response side only. The adapter adds the request side. */
	public static LlmCallMetadata from(ChatResponse response, String requestedModel) {
		return from(response, requestedModel, null, null);
	}

	public static LlmCallMetadata from(ChatResponse response, String requestedModel, String prompt,
			Map<String, Object> requestParams) {
		if (response == null) {
			return LlmCallMetadata.requestOnly(requestedModel, prompt, requestParams);
		}
		String raw = response.aiMessage() != null ? response.aiMessage().text() : null;
		String model = requestedModel;
		String finishReason = null;
		if (response.metadata() != null) {
			if (response.metadata().modelName() != null) {
				model = response.metadata().modelName();
			}
			if (response.metadata().finishReason() != null) {
				finishReason = response.metadata().finishReason().name();
			}
		}
		Integer in = response.tokenUsage() != null ? response.tokenUsage().inputTokenCount() : null;
		Integer out = response.tokenUsage() != null ? response.tokenUsage().outputTokenCount() : null;
		return new LlmCallMetadata(model, prompt, requestParams, raw, in, out, finishReason);
	}

	/**
	 * Metadata for a failed call. Prefers the response the exception carries (for
	 * example a response cut off at max tokens) over {@code response}.
	 */
	public static LlmCallMetadata fromFailure(Exception e, ChatResponse response, String requestedModel,
			String prompt, Map<String, Object> requestParams) {
		if (e instanceof LlmResponseParsingException parsing && parsing.getCall() != null) {
			return parsing.getCall().withRequest(prompt, requestParams);
		}
		return from(response, requestedModel, prompt, requestParams);
	}
}
