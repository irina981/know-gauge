package com.knowgauge.infra.testgeneration.langchain4j.openai.service;

import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public interface OpenAiClient {
	
	ChatResponse callLlm(String prompt, Test test, List<TestQuestion> questions, OpenAiChatModel chatModel, String modelName);
	
	default ChatResponse callLlm(String prompt, Test test, OpenAiChatModel chatModel, String modelName) {
		return callLlm(prompt, test, null, chatModel, modelName);
	}
	
	boolean supportsStructuredOutputResponseFormat(String modelName);

	/** The prompt text {@link #callLlm} actually sends for {@code prompt}. */
	String effectivePrompt(String prompt);

	/**
	 * Response-format settings {@link #callLlm} uses for this model: the format
	 * and, for structured output, the output template the JSON schema is built
	 * from. For the generation run record.
	 */
	Map<String, Object> responseFormatParams(String modelName);

}
