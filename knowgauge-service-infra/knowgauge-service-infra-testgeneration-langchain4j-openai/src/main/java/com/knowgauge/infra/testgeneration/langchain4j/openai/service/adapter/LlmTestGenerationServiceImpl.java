package com.knowgauge.infra.testgeneration.langchain4j.openai.service.adapter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.testgeneration.LlmGenerationResult;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.LlmCallMetadataMapper;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OpenAiChatModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.TestGenerationResponseMapper;
import com.knowgauge.infra.testgeneration.langchain4j.openai.service.OpenAiClient;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LlmTestGenerationServiceImpl implements LlmTestGenerationService {
	private final OpenAiClient openAiClient;
	private final OpenAiChatModelProperties chatModelProperties;
	private final TestGenerationResponseMapper responseMapper;

	public LlmTestGenerationServiceImpl(@Qualifier("testGenerationOpenAiClient") OpenAiClient openAiClient, 
			OpenAiChatModelProperties chatModelProperties,
			TestGenerationResponseMapper responseMapper) {
		this.openAiClient = openAiClient;
		this.chatModelProperties = chatModelProperties;
		this.responseMapper = responseMapper;
	}

	@Override
	public LlmGenerationResult generate(String prompt, Test test) {
		String model = resolveModelName(test);
		String sentPrompt = openAiClient.effectivePrompt(prompt);
		Map<String, Object> requestParams = requestParams(test, model);
		ChatResponse response = null;
		try {
			response = openAiClient.callLlm(prompt, test, buildChatModel(test), model);
			List<TestQuestion> questions = responseMapper.map(response, test);
			return new LlmGenerationResult(questions,
					LlmCallMetadataMapper.from(response, model, sentPrompt, requestParams));
		} catch (LlmResponseParsingException e) {
			throw e.withCall(LlmCallMetadataMapper.fromFailure(e, response, model, sentPrompt, requestParams));
		}
	}

	/** Settings recorded with the generation run. Mirrors {@link #buildChatModel}. */
	private Map<String, Object> requestParams(Test test, String model) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("temperature", resolveTemperature(test));
		params.put("maxOutputTokens", resolveMaxOutputTokens(test));
		params.put("strictJsonSchema", openAiClient.supportsStructuredOutputResponseFormat(model));
		params.putAll(openAiClient.responseFormatParams(model));
		return params;
	}

	private Double resolveTemperature(Test test) {
		Double temperature = resolveOptionalDoubleParam(test, "temperature");
		return temperature != null ? temperature : chatModelProperties.getTemperature();
	}

	private Integer resolveMaxOutputTokens(Test test) {
		Integer maxOutputTokens = resolveOptionalIntParam(test, "maxOutputTokens");
		return maxOutputTokens != null ? maxOutputTokens : chatModelProperties.getMaxOutputTokens();
	}

	protected OpenAiChatModel buildChatModel(Test test) {
		String model = resolveModelName(test);

		Double temperature = resolveTemperature(test);
		Integer maxOutputTokens = resolveMaxOutputTokens(test);

		boolean supportsStructuredOutput = openAiClient.supportsStructuredOutputResponseFormat(model);

		OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
				.apiKey(chatModelProperties.getApiKey()).modelName(model).strictJsonSchema(supportsStructuredOutput);

		if (temperature != null) {
			builder.temperature(temperature);
		}

		if (maxOutputTokens != null) {
			builder.maxTokens(maxOutputTokens);
		}

		if (chatModelProperties.getTimeoutSeconds() != null) {
			builder.timeout(Duration.ofSeconds(chatModelProperties.getTimeoutSeconds()));
		}

		return builder.build();
	}

	protected String resolveModelName(Test test) {
		return (test.getGenerationModel() != null && !test.getGenerationModel().isBlank()) ? test.getGenerationModel()
				: chatModelProperties.getModel();
	}

	private Double resolveOptionalDoubleParam(Test test, String key) {
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get(key) != null) {
			Object raw = params.get(key);
			if (raw instanceof Number number) {
				return number.doubleValue();
			}
			return Double.parseDouble(String.valueOf(raw));
		}
		return null;
	}

	private Integer resolveOptionalIntParam(Test test, String key) {
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get(key) != null) {
			Object raw = params.get(key);
			if (raw instanceof Number number) {
				return number.intValue();
			}
			return Integer.parseInt(String.valueOf(raw));
		}
		return null;
	}
}
