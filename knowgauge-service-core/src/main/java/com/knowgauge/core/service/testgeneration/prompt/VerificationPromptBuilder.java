package com.knowgauge.core.service.testgeneration.prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.AnswerOption;
import com.knowgauge.core.service.testgeneration.schema.SchemaProvider;

@Component
public class VerificationPromptBuilder {
	private final PromptTemplateLoader templateLoader;
	private final PromptTemplateRenderer templateRenderer;
	private final SchemaProvider schemaProvider;
	private final String defaultTemplateId;
	private final ObjectMapper objectMapper;

	public VerificationPromptBuilder(PromptTemplateLoader templateLoader, PromptTemplateRenderer templateRenderer,
			@Qualifier("verificationOutputSchemaProvider") SchemaProvider schemaProvider,
			@Value("${kg.testgen.verification.defaults.prompt-template-id}") String defaultTemplateId) {
		super();
		this.templateLoader = templateLoader;
		this.templateRenderer = templateRenderer;
		this.schemaProvider = schemaProvider;
		this.defaultTemplateId = defaultTemplateId;
		this.objectMapper = new ObjectMapper();
	}

	public String buildPrompt(List<TestQuestion> questions, Test test) {
		Objects.requireNonNull(test, "Test must not be null.");
		Objects.requireNonNull(questions, "Chunks must not be null.");

		String template = templateLoader.loadTemplate(defaultTemplateId);

		Map<String, Object> vars = new LinkedHashMap<>();

		List<QuestionInput> inputQuestions = buildInputQuestions(questions);
		try {
			String inputJson = objectMapper.writeValueAsString(Map.of("questions", inputQuestions));
			vars.put("input", inputJson);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(
					String.format("Test {} - Failed to convert questions: {} to JSON", test.getId(), questions), e);
		}
		vars.put("outputSchema", schemaProvider.getOutputSchemaJson());

		return templateRenderer.render(template, vars);
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

	private String safe(Object v) {
		return v == null ? "" : String.valueOf(v);
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

}
