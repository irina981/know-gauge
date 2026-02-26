package com.knowgauge.infra.testgeneration.langchain4j.openai.service.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.port.testgeneration.LlmIncorrectOptionsVerificationService;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OpenAiChatModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OpenAiVerificationModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.VerificationResponseMapper;
import com.knowgauge.infra.testgeneration.langchain4j.openai.service.OpenAiClient;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LlmIncorrectOptionsVerificationServiceImpl implements LlmIncorrectOptionsVerificationService {

	private final OpenAiClient openAiClient;
	private final OpenAiVerificationModelProperties verificationModelProperties;
	private final OpenAiVerificationModelProperties fallbackProperties;
	private final VerificationResponseMapper responseMapper;

	public LlmIncorrectOptionsVerificationServiceImpl(@Qualifier("verificationOpenAiClient") OpenAiClient openAiClient,
			OpenAiVerificationModelProperties verificationModelProperties,
			OpenAiChatModelProperties chatModelProperties, VerificationResponseMapper responseMapper) {

		this.openAiClient = openAiClient;
		this.verificationModelProperties = verificationModelProperties;
		this.responseMapper = responseMapper;

		// Create fallback properties from main chat model properties
		this.fallbackProperties = new OpenAiVerificationModelProperties();
		this.fallbackProperties.setApiKey(chatModelProperties.getApiKey());
		this.fallbackProperties.setModel(chatModelProperties.getModel());
		this.fallbackProperties.setTemperature(chatModelProperties.getTemperature());
		this.fallbackProperties.setMaxOutputTokens(chatModelProperties.getMaxOutputTokens());
		this.fallbackProperties.setTimeoutSeconds(chatModelProperties.getTimeoutSeconds());
	}

	public int verifyAndReplaceUnsafeOptions(String prompt, Test test, List<TestQuestion> questions) {
		if (prompt == null || prompt.isBlank()) {
			log.warn("Test {} - Prompt is empty, verification cannot be done.");
			return 0;
		}

		// Only verify for MULTIPLE_CORRECT cardinality
		if (test.getAnswerCardinality() != AnswerCardinality.MULTIPLE_CORRECT) {
			return 0;
		}

		if (questions == null || questions.isEmpty()) {
			return 0;
		}

		try {
			ChatResponse response = openAiClient.callLlm(prompt, test, buildChatModel(test), resolveModelName(test));
			return responseMapper.mapAndApplyReplacements(response, questions, test);
		} catch (Exception e) {
			log.error("Failed to verify incorrect options for test {}: {}", test.getId(), e.getMessage(), e);
			return 0;
		}
	}

	protected OpenAiChatModel buildChatModel(Test test) {
		String model = resolveModelName(test);
		String apiKey = resolveApiKey();

		Double temperature = resolveTemperature(test);
		Integer maxOutputTokens = resolveMaxOutputTokens(test);
		Integer timeoutSeconds = resolveTimeoutSeconds();

		OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder().apiKey(apiKey).modelName(model);

		if (temperature != null) {
			builder.temperature(temperature);
		}

		if (maxOutputTokens != null) {
			builder.maxTokens(maxOutputTokens);
		}

		if (timeoutSeconds != null) {
			builder.timeout(Duration.ofSeconds(timeoutSeconds));
		}

		return builder.build();
	}

	protected String resolveModelName(Test test) {
		// Check for test-level override
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get("verificationModel") != null) {
			return String.valueOf(params.get("verificationModel"));
		}

		// Use verification-specific model if configured
		if (verificationModelProperties.getModel() != null && !verificationModelProperties.getModel().isBlank()) {
			return verificationModelProperties.getModel();
		}

		// Fallback to main chat model
		return fallbackProperties.getModel();
	}

	private String resolveApiKey() {
		if (verificationModelProperties.getApiKey() != null && !verificationModelProperties.getApiKey().isBlank()) {
			return verificationModelProperties.getApiKey();
		}
		return fallbackProperties.getApiKey();
	}

	private Double resolveTemperature(Test test) {
		// Check test-level override
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get("verificationTemperature") != null) {
			Object raw = params.get("verificationTemperature");
			if (raw instanceof Number number) {
				return number.doubleValue();
			}
		}

		if (verificationModelProperties.getTemperature() != null) {
			return verificationModelProperties.getTemperature();
		}

		return fallbackProperties.getTemperature();
	}

	private Integer resolveMaxOutputTokens(Test test) {
		// Check test-level override
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get("verificationMaxOutputTokens") != null) {
			Object raw = params.get("verificationMaxOutputTokens");
			if (raw instanceof Number number) {
				return number.intValue();
			}
		}

		if (verificationModelProperties.getMaxOutputTokens() != null) {
			return verificationModelProperties.getMaxOutputTokens();
		}

		return fallbackProperties.getMaxOutputTokens();
	}

	private Integer resolveTimeoutSeconds() {
		if (verificationModelProperties.getTimeoutSeconds() != null) {
			return verificationModelProperties.getTimeoutSeconds();
		}
		return fallbackProperties.getTimeoutSeconds();
	}

}
