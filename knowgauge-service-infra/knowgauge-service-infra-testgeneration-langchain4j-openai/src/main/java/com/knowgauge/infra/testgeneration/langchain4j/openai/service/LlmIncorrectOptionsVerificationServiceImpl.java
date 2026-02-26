package com.knowgauge.infra.testgeneration.langchain4j.openai.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.AnswerOption;
import com.knowgauge.core.port.testgeneration.LlmIncorrectOptionsVerificationService;
import com.knowgauge.core.service.testgeneration.prompt.PromptTemplateLoader;
import com.knowgauge.core.service.testgeneration.prompt.PromptTemplateRenderer;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OpenAiChatModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OpenAiVerificationModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.VerificationResponseMapper;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LlmIncorrectOptionsVerificationServiceImpl implements LlmIncorrectOptionsVerificationService {

	private final OpenAiVerificationModelProperties verificationModelProperties;
	private final OpenAiVerificationModelProperties fallbackProperties;
	private final ObjectMapper objectMapper;
	private final PromptTemplateLoader templateLoader;
	private final PromptTemplateRenderer templateRenderer;
	private final VerificationResponseMapper responseMapper;

	public LlmIncorrectOptionsVerificationServiceImpl(OpenAiVerificationModelProperties verificationModelProperties,
			OpenAiChatModelProperties fallbackProperties, PromptTemplateLoader templateLoader,
			PromptTemplateRenderer templateRenderer, VerificationResponseMapper responseMapper) {
		this.verificationModelProperties = verificationModelProperties;

		// Create fallback properties from main chat model properties
		this.fallbackProperties = new OpenAiVerificationModelProperties();
		this.fallbackProperties.setApiKey(fallbackProperties.getApiKey());
		this.fallbackProperties.setModel(fallbackProperties.getModel());
		this.fallbackProperties.setTemperature(fallbackProperties.getTemperature());
		this.fallbackProperties.setMaxOutputTokens(fallbackProperties.getMaxOutputTokens());
		this.fallbackProperties.setTimeoutSeconds(fallbackProperties.getTimeoutSeconds());

		this.objectMapper = new ObjectMapper();
		this.templateLoader = templateLoader;
		this.templateRenderer = templateRenderer;
		this.responseMapper = responseMapper;
	}

	@Override
	public int verifyAndReplaceUnsafeOptions(List<TestQuestion> questions, Test test) {
		// Only verify for MULTIPLE_CORRECT cardinality
		if (test.getAnswerCardinality() != AnswerCardinality.MULTIPLE_CORRECT) {
			return 0;
		}

		if (questions == null || questions.isEmpty()) {
			return 0;
		}

		try {
			// Build input payload
			List<QuestionInput> inputQuestions = buildInputQuestions(questions);
			String inputJson = objectMapper.writeValueAsString(Map.of("questions", inputQuestions));

			// Load and render prompt
			String template = templateLoader.loadTemplate("incorrect-options-verification");
			Map<String, Object> variables = Map.of("input", inputJson);
			String prompt = templateRenderer.render(template, variables);
			log.debug("    Incorrect options verification for test {} - Built prompt for {} questions: {}",
					test.getId(), questions.size(), prompt);

			// Call LLM
			OpenAiChatModel chatModel = buildChatModel(test);
			ChatRequest request = ChatRequest.builder().messages(List.of(UserMessage.from(prompt))).build();

			ChatResponse response = chatModel.chat(request);

			// Log usage
			logResponseMetadata(response, test);

			// Parse response and apply replacements
			int replacedCount = responseMapper.mapAndApplyReplacements(response, questions, test);

			return replacedCount;

		} catch (Exception e) {
			log.error("Failed to verify incorrect options for test {}: {}", test.getId(), e.getMessage(), e);
			return 0;
		}
	}

	private List<QuestionInput> buildInputQuestions(List<TestQuestion> questions) {
		List<QuestionInput> inputQuestions = new ArrayList<>();

		for (int i = 0; i < questions.size(); i++) {
			TestQuestion q = questions.get(i);
			QuestionInput input = new QuestionInput();
			input.questionIndex = i;
			input.questionText = q.getQuestionText();
			input.answers = new HashMap<>();
			List<AnswerOption> correctOptions = q.getCorrectOptions();
			for (AnswerOption option : AnswerOption.values()) {
				if (!correctOptions.contains(option)) {
					input.answers.put(option.toString(), getOptionText(q, option));
				}
			}

			inputQuestions.add(input);
		}

		return inputQuestions;
	}

	private String getOptionText(TestQuestion q, AnswerOption option) {
		return switch (option) {
		case A -> q.getOptionA();
		case B -> q.getOptionB();
		case C -> q.getOptionC();
		case D -> q.getOptionD();
		};
	}

	private OpenAiChatModel buildChatModel(Test test) {
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

	private String resolveModelName(Test test) {
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

	private void logResponseMetadata(ChatResponse response, Test test) {
		try {
			if (response != null && response.metadata() != null) {
				Object usage = response.tokenUsage();
				Object finishReason = response.metadata().finishReason();

				if (usage != null || finishReason != null) {
					log.info("LLM Verification Response (testId={}): usage={}, finishReason={}", test.getId(), usage,
							finishReason);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to log LLM verification response metadata for testId={}", test.getId(), e);
		}
	}

	// Input DTO for JSON serialization
	private static class QuestionInput {
		@JsonProperty("questionIndex")
		public int questionIndex;

		@JsonProperty("questionText")
		public String questionText;

		@JsonProperty("answers")
		public Map<String, String> answers;
	}
}
