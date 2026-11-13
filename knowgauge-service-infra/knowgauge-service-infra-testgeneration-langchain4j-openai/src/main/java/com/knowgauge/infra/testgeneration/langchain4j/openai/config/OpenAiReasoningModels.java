package com.knowgauge.infra.testgeneration.langchain4j.openai.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Request settings for OpenAI reasoning models such as the GPT-5 family, which reject a custom temperature and the
 * {@code max_tokens} parameter. For them the chat model gets no temperature, {@code max_completion_tokens} (which
 * also covers the hidden reasoning tokens) and a reasoning effort. Every other model keeps temperature and
 * {@code max_tokens} exactly as configured.
 */
@Component
public class OpenAiReasoningModels {

	private final List<String> prefixes;
	private final String reasoningEffort;
	private final Integer maxCompletionTokens;

	public OpenAiReasoningModels(
			@Value("${kg.testgen.chat-model.openai.reasoning-model-prefixes:gpt-5}") List<String> prefixes,
			@Value("${kg.testgen.chat-model.openai.reasoning-effort:low}") String reasoningEffort,
			@Value("${kg.testgen.chat-model.openai.reasoning-max-completion-tokens:8000}") Integer maxCompletionTokens) {
		this.prefixes = prefixes != null ? prefixes : List.of();
		this.reasoningEffort = reasoningEffort;
		this.maxCompletionTokens = maxCompletionTokens;
	}

	public boolean isReasoningModel(String modelName) {
		if (modelName == null) {
			return false;
		}
		String lower = modelName.toLowerCase(Locale.ROOT);
		return prefixes.stream().filter(p -> p != null && !p.isBlank())
				.anyMatch(p -> lower.startsWith(p.trim().toLowerCase(Locale.ROOT)));
	}

	/**
	 * Sets temperature and output limit on the builder: unchanged for ordinary models, reasoning settings for
	 * reasoning models.
	 */
	public void configure(OpenAiChatModel.OpenAiChatModelBuilder builder, String modelName, Double temperature,
			Integer maxOutputTokens) {
		if (isReasoningModel(modelName)) {
			if (maxCompletionTokens != null) {
				builder.maxCompletionTokens(maxCompletionTokens);
			}
			if (reasoningEffort != null && !reasoningEffort.isBlank()) {
				builder.reasoningEffort(reasoningEffort.trim());
			}
			return;
		}
		if (temperature != null) {
			builder.temperature(temperature);
		}
		if (maxOutputTokens != null) {
			builder.maxTokens(maxOutputTokens);
		}
	}

	/** The settings {@link #configure} applies, for the recorded LLM call. */
	public Map<String, Object> requestParams(String modelName, Double temperature, Integer maxOutputTokens) {
		Map<String, Object> params = new LinkedHashMap<>();
		if (isReasoningModel(modelName)) {
			params.put("maxCompletionTokens", maxCompletionTokens);
			params.put("reasoningEffort", reasoningEffort);
			return params;
		}
		params.put("temperature", temperature);
		params.put("maxOutputTokens", maxOutputTokens);
		return params;
	}
}
