package com.knowgauge.infra.testgeneration.langchain4j.openai.service;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.core.service.testgeneration.schema.TestQuestionSchemaProvider;
import com.knowgauge.infra.testgeneration.langchain4j.openai.config.OpenAiChatModelProperties;
import com.knowgauge.infra.testgeneration.langchain4j.openai.mapper.ChatResponseMapper;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNullSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LlmTestGenerationServiceImpl implements LlmTestGenerationService {

	private final OpenAiChatModelProperties chatModelProperties;
	private final ChatResponseMapper responseMapper;
	private final TestQuestionSchemaProvider schemaProvider;
	private final List<String> structuredOutputModelPrefixes;
	private final ObjectMapper objectMapper;

	public LlmTestGenerationServiceImpl(OpenAiChatModelProperties chatModelProperties,
			ChatResponseMapper responseMapper,
			TestQuestionSchemaProvider schemaProvider,
			@Value("${kg.testgen.chat-model.openai.structured-output-model-prefixes:gpt-4.1,gpt-4o,o1,o3,o4}") List<String> structuredOutputModelPrefixes) {
		this.chatModelProperties = chatModelProperties;
		this.responseMapper = responseMapper;
		this.schemaProvider = schemaProvider;
		this.structuredOutputModelPrefixes = structuredOutputModelPrefixes;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public List<TestQuestion> generate(String prompt, Test test) {
		if (prompt == null || prompt.isBlank()) {
			return Collections.emptyList();
		}

		OpenAiChatModel chatModel = buildChatModel(test);
		String modelName = resolveModelName(test);
		boolean supportsStructuredOutput = supportsStructuredOutputResponseFormat(modelName);

		boolean strictJson = resolveStrictJsonOrDefault(test);
		String effectivePrompt = strictJson ? enforceJsonOnly(prompt) : prompt;
		String schemaJson = schemaProvider.getOutputSchemaJson();
		if (schemaJson == null || schemaJson.isBlank()) {
			throw new IllegalStateException("Output schema JSON must not be empty.");
		}

		ChatRequest request = buildRequest(effectivePrompt, schemaJson, strictJson && supportsStructuredOutput);
		ChatResponse response = chatModel.chat(request);

		checkFinishReasonForErrors(response, test);

		// Log usage and response metadata
		if (response != null) {
			logResponseMetadata(response, test);
		}

		return responseMapper.map(response, test);
	}

	private void checkFinishReasonForErrors(ChatResponse response, Test test) {
		if (response != null && response.metadata() != null) {
			Object finishReasonObj = response.metadata().finishReason();
			if (finishReasonObj != null) {
				String finishReason = finishReasonObj.toString().toLowerCase(Locale.ROOT);
				if ("length".equals(finishReason)) {
					throw new LlmResponseParsingException(
							LlmResponseParsingException.Reason.LENGTH,
							"LLM response generation stopped due to max output tokens limit (testId=" + test.getId()
									+ ")");
				}
			}
		}
	}

	private void logResponseMetadata(ChatResponse response, Test test) {
		try {
			if (response.metadata() != null) {
				Object usage = response.tokenUsage();
				Object finishReason = response.metadata().finishReason();

				if (usage != null || finishReason != null) {
					log.info("LLM Response (testId={}): usage={}, finishReason={}", test.getId(), usage, finishReason);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to log LLM response metadata for testId={}", test.getId(), e);
		}
	}

	private ChatRequest buildRequest(String effectivePrompt, String schemaJson, boolean useStructuredOutput) {
		ChatRequest.Builder builder = ChatRequest.builder().messages(List.of(UserMessage.from(effectivePrompt)));
		if (useStructuredOutput) {
			JsonSchema responseJsonSchema = toResponseJsonSchema(schemaJson);
			builder.parameters(ChatRequestParameters.builder().responseFormat(responseJsonSchema).build());
		}
		return builder.build();
	}

	private String enforceJsonOnly(String basePrompt) {
		return basePrompt + "\n\nReturn ONLY a valid JSON array. No markdown. No explanations.";
	}

	private JsonSchema toResponseJsonSchema(String schemaJson) {
		try {
			JsonNode rootNode = objectMapper.readTree(schemaJson);
			return JsonSchema.builder()
					.name("output_schema")
					.rootElement(inferElement(rootNode))
					.build();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to parse output schema JSON for chat response format.", e);
		}
	}

	private JsonSchemaElement inferElement(JsonNode node) {
		if (node == null || node.isNull()) {
			return new JsonNullSchema();
		}

		if (node.isObject()) {
			JsonObjectSchema.Builder builder = JsonObjectSchema.builder().additionalProperties(false);
			Iterator<String> fieldNames = node.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				builder.addProperty(fieldName, inferElement(node.get(fieldName)));
			}

			builder.required(node.properties().stream().map(Map.Entry::getKey).toArray(String[]::new));
			return builder.build();
		}

		if (node.isArray()) {
			JsonSchemaElement itemElement = node.isEmpty()
					? JsonStringSchema.builder().build()
					: inferElement(node.get(0));
			return JsonArraySchema.builder().items(itemElement).build();
		}

		if (node.isIntegralNumber()) {
			return JsonIntegerSchema.builder().build();
		}

		if (node.isNumber()) {
			return JsonNumberSchema.builder().build();
		}

		if (node.isBoolean()) {
			return JsonBooleanSchema.builder().build();
		}

		return JsonStringSchema.builder().build();
	}

	private OpenAiChatModel buildChatModel(Test test) {
		String model = resolveModelName(test);

		Double temperature = resolveOptionalDoubleParam(test, "temperature");
		if (temperature == null) {
			temperature = chatModelProperties.getTemperature();
		}

		Integer maxOutputTokens = resolveOptionalIntParam(test, "maxOutputTokens");
		if (maxOutputTokens == null) {
			maxOutputTokens = chatModelProperties.getMaxOutputTokens();
		}

		Boolean strictJson = resolveStrictJsonOrDefault(test);
		boolean supportsStructuredOutput = supportsStructuredOutputResponseFormat(model);

		OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
				.apiKey(chatModelProperties.getApiKey()).modelName(model).strictJsonSchema(strictJson && supportsStructuredOutput);

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

	private String resolveModelName(Test test) {
		return (test.getGenerationModel() != null && !test.getGenerationModel().isBlank())
				? test.getGenerationModel()
				: chatModelProperties.getModel();
	}

	private boolean supportsStructuredOutputResponseFormat(String modelName) {
		if (modelName == null) {
			return false;
		}

		List<String> prefixes = structuredOutputModelPrefixes;
		if (prefixes == null || prefixes.isEmpty()) {
			prefixes = List.of("gpt-4.1", "gpt-4o", "o1", "o3", "o4");
		}

		String lowerModelName = modelName.toLowerCase(Locale.ROOT);
		for (String prefix : prefixes) {
			if (prefix != null && !prefix.isBlank()
					&& lowerModelName.startsWith(prefix.toLowerCase(Locale.ROOT).trim())) {
				return true;
			}
		}

		return false;
	}

	private boolean resolveStrictJsonOrDefault(Test test) {
		Boolean strictJson = resolveStrictJson(test);
		if (strictJson != null) {
			return strictJson;
		}
		return Boolean.TRUE.equals(chatModelProperties.getStrictJson());
	}

	private Boolean resolveStrictJson(Test test) {
		Map<String, Object> params = test.getGenerationParams();
		if (params != null && params.get("strictJson") != null) {
			Object raw = params.get("strictJson");
			if (raw instanceof Boolean value) {
				return value;
			}
			return Boolean.parseBoolean(String.valueOf(raw));
		}
		return null;
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
