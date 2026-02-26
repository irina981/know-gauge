package com.knowgauge.infra.testgeneration.langchain4j.openai.service.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
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
	public List<TestQuestion> generate(String prompt, Test test) {
		ChatResponse response = openAiClient.callLlm(prompt, test, buildChatModel(test), resolveModelName(test));
		return responseMapper.map(response, test);
	}

	protected OpenAiChatModel buildChatModel(Test test) {
		String model = resolveModelName(test);

		Double temperature = resolveOptionalDoubleParam(test, "temperature");
		if (temperature == null) {
			temperature = chatModelProperties.getTemperature();
		}

		Integer maxOutputTokens = resolveOptionalIntParam(test, "maxOutputTokens");
		if (maxOutputTokens == null) {
			maxOutputTokens = chatModelProperties.getMaxOutputTokens();
		}

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
