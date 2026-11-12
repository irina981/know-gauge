package com.knowgauge.infra.testgeneration.langchain4j.openai.mapper;

import com.knowgauge.core.port.testgeneration.LlmCallMetadata;

import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * Extracts what the LLM call did (model, raw text, token usage, finish reason)
 * for the generation run record.
 */
public final class LlmCallMetadataMapper {

	private LlmCallMetadataMapper() {
	}

	public static LlmCallMetadata from(ChatResponse response, String requestedModel) {
		if (response == null) {
			return new LlmCallMetadata(requestedModel, null, null, null, null);
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
		return new LlmCallMetadata(model, raw, in, out, finishReason);
	}
}
