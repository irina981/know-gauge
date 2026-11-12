package com.knowgauge.infra.testgeneration.langchain4j.openai.service.adapter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.testgeneration.LlmIncorrectOptionsVerificationService;
import com.knowgauge.core.port.testgeneration.LlmVerificationResult;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OllamaVerificationModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.LlmCallMetadataMapper;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.VerificationResponseMapper;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.FinishReason;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifies distractors with a local Ollama model, so this pass costs no OpenAI
 * tokens. Uses the same prompt and JSON reply as the OpenAI verifier, so flags,
 * replacement text and reasons work the same way.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "kg.testgen.verification.provider", havingValue = "ollama")
public class OllamaIncorrectOptionsVerificationServiceImpl implements LlmIncorrectOptionsVerificationService {

	private static final String JSON_ONLY_SUFFIX = "\n\nReturn ONLY a valid JSON object. No markdown. No explanations.";

	private final OllamaVerificationModelProperties properties;
	private final VerificationResponseMapper responseMapper;

	public OllamaIncorrectOptionsVerificationServiceImpl(OllamaVerificationModelProperties properties,
			VerificationResponseMapper responseMapper) {
		this.properties = properties;
		this.responseMapper = responseMapper;
		log.info("Distractor verification uses Ollama model '{}' at {}", properties.getModel(),
				properties.getBaseUrl());
	}

	@Override
	public LlmVerificationResult verify(String prompt, Test test, List<TestQuestion> questions) {
		if (prompt == null || prompt.isBlank()) {
			log.warn("Test {} - Prompt is empty, verification cannot be done.", test.getId());
			return new LlmVerificationResult(0, null, "Verification prompt is empty");
		}

		if (questions == null || questions.isEmpty()) {
			return new LlmVerificationResult(0, null, null);
		}

		String model = resolveModelName(test);
		String effectivePrompt = prompt + JSON_ONLY_SUFFIX;
		Map<String, Object> requestParams = requestParams(test);
		ChatResponse response = null;
		try {
			log.info("Ollama verification prompt (testId={}, model={}, questions={}):\n{}", test.getId(), model,
					questions.size(), effectivePrompt);
			ChatRequest request = ChatRequest.builder().messages(List.of(UserMessage.from(effectivePrompt))).build();
			response = buildChatModel(test, model).chat(request);
			log.info("Ollama verification response (testId={}, model={}):\n{}", test.getId(), model,
					response != null && response.aiMessage() != null ? response.aiMessage().text() : null);
			checkFinishReason(response, test);
			if (response.tokenUsage() != null) {
				log.info("Ollama verification response (testId={}): usage={}", test.getId(), response.tokenUsage());
			}
			int flagged = responseMapper.mapAndApplyVerificationResult(response, questions, test);
			return new LlmVerificationResult(flagged,
					LlmCallMetadataMapper.from(response, model, effectivePrompt, requestParams), null);
		} catch (Exception e) {
			log.error("Failed to verify incorrect options with Ollama model {} for test {}: {}", model, test.getId(),
					e.getMessage(), e);
			return new LlmVerificationResult(0,
					LlmCallMetadataMapper.fromFailure(e, response, model, effectivePrompt, requestParams),
					"Ollama verification failed (" + properties.getBaseUrl() + "): "
							+ (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
		}
	}

	/** Settings recorded with the verification run. Mirrors {@link #buildChatModel}. */
	private Map<String, Object> requestParams(Test test) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("provider", "ollama");
		params.put("temperature", resolveDouble(test, "verificationTemperature", properties.getTemperature()));
		params.put("maxOutputTokens",
				resolveInteger(test, "verificationMaxOutputTokens", properties.getMaxOutputTokens()));
		params.put("contextLength", properties.getContextLength());
		params.put("responseFormat", "JSON");
		return params;
	}

	private OllamaChatModel buildChatModel(Test test, String model) {
		OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder().baseUrl(properties.getBaseUrl())
				.modelName(model).responseFormat(ResponseFormat.JSON)
				// A local model that timed out will time out again; a retry only doubles the wait.
				.maxRetries(0);

		Double temperature = resolveDouble(test, "verificationTemperature", properties.getTemperature());
		if (temperature != null) {
			builder.temperature(temperature);
		}

		Integer maxOutputTokens = resolveInteger(test, "verificationMaxOutputTokens", properties.getMaxOutputTokens());
		if (maxOutputTokens != null) {
			builder.numPredict(maxOutputTokens);
		}

		if (properties.getContextLength() != null) {
			builder.numCtx(properties.getContextLength());
		}

		if (properties.getTimeoutSeconds() != null) {
			builder.timeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
		}

		return builder.build();
	}

	private String resolveModelName(Test test) {
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get("verificationModel") != null) {
			return String.valueOf(params.get("verificationModel"));
		}
		return properties.getModel();
	}

	private static Double resolveDouble(Test test, String key, Double fallback) {
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get(key) instanceof Number number) {
			return number.doubleValue();
		}
		return fallback;
	}

	private static Integer resolveInteger(Test test, String key, Integer fallback) {
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get(key) instanceof Number number) {
			return number.intValue();
		}
		return fallback;
	}

	private static void checkFinishReason(ChatResponse response, Test test) {
		if (response != null && response.metadata() != null
				&& response.metadata().finishReason() == FinishReason.LENGTH) {
			throw new LlmResponseParsingException(LlmResponseParsingException.Reason.LENGTH,
					"Ollama verification stopped due to max output tokens limit (testId=" + test.getId() + ")");
		}
	}
}
