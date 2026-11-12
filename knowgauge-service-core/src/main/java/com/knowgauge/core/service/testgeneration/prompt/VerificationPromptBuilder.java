package com.knowgauge.core.service.testgeneration.prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.enums.OptionLetter;
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

	public String getTemplateId() {
		return defaultTemplateId;
	}

	/**
	 * @param questions questions whose distractors are verified
	 * @param test      test being generated
	 * @param chunks    grounding chunks of the batch. Only the chunks the
	 *                  questions cite are sent, so the verifier judges answers
	 *                  against the source text instead of general knowledge.
	 */
	public String buildPrompt(List<TestQuestion> questions, Test test, List<DocumentChunk> chunks) {
		Objects.requireNonNull(test, "Test must not be null.");
		Objects.requireNonNull(questions, "Questions must not be null.");

		String template = templateLoader.loadTemplate(defaultTemplateId);

		Map<String, Object> vars = new LinkedHashMap<>();

		List<QuestionInput> inputQuestions = buildInputQuestions(questions);
		List<SourceInput> sources = buildSources(questions, chunks);
		try {
			Map<String, Object> input = new LinkedHashMap<>();
			input.put("sources", sources);
			input.put("questions", inputQuestions);
			String inputJson = objectMapper.writeValueAsString(input);
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

		@JsonProperty("sourceChunkIds")
		public List<Long> sourceChunkIds;

		// The answer key, as context only: it shows how the question is meant and
		// keeps replacements from repeating a correct answer. It is not judged.
		@JsonProperty("correctAnswers")
		public List<String> correctAnswers;

		@JsonProperty("answers")
		public Map<String, String> answers;
	}

	private static class SourceInput {
		@JsonProperty("chunkId")
		public Long chunkId;

		@JsonProperty("text")
		public String text;
	}

	/**
	 * Chunks cited by the questions, each once, in first-cited order. Citations
	 * to chunks outside the batch are skipped.
	 */
	private List<SourceInput> buildSources(List<TestQuestion> questions, List<DocumentChunk> chunks) {
		if (chunks == null || chunks.isEmpty()) {
			return List.of();
		}
		Map<Long, DocumentChunk> byId = new HashMap<>();
		for (DocumentChunk chunk : chunks) {
			if (chunk != null && chunk.getId() != null) {
				byId.putIfAbsent(chunk.getId(), chunk);
			}
		}
		Set<Long> cited = new LinkedHashSet<>();
		for (TestQuestion q : questions) {
			if (q.getSourceChunkIds() != null) {
				q.getSourceChunkIds().stream().filter(Objects::nonNull).forEach(cited::add);
			}
		}
		List<SourceInput> sources = new ArrayList<>();
		for (Long id : cited) {
			DocumentChunk chunk = byId.get(id);
			if (chunk != null) {
				SourceInput source = new SourceInput();
				source.chunkId = id;
				source.text = chunk.getChunkText();
				sources.add(source);
			}
		}
		return sources;
	}

	private List<QuestionInput> buildInputQuestions(List<TestQuestion> questions) {
		List<QuestionInput> inputQuestions = new ArrayList<>();

		for (int i = 0; i < questions.size(); i++) {
			TestQuestion q = questions.get(i);
			QuestionInput input = new QuestionInput();
			input.questionIndex = i;
			input.questionText = q.getQuestionText();
			input.sourceChunkIds = q.getSourceChunkIds() != null ? q.getSourceChunkIds() : List.of();
			input.correctAnswers = new ArrayList<>();
			input.answers = new TreeMap<>();
			// Only distractors (options not in the answer key) are verified
			for (OptionLetter letter : OptionLetter.values()) {
				TestQuestionOption option = q.getOption(letter);
				if (option == null) {
					continue;
				}
				if (option.isCorrect()) {
					input.correctAnswers.add(option.getText());
				} else {
					input.answers.put(letter.toString(), option.getText());
				}
			}

			inputQuestions.add(input);
		}

		return inputQuestions;
	}

}
