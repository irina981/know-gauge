package com.knowgauge.core.service.testgeneration.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.service.testgeneration.schema.SchemaProvider;

@Component
public class TestGenerationPromptBuilder {
	private final PromptTemplateLoader templateLoader;
	private final PromptTemplateRenderer templateRenderer;
	private final SchemaProvider schemaProvider;
	private final String defaultTemplateId;

	public TestGenerationPromptBuilder(PromptTemplateLoader templateLoader, PromptTemplateRenderer templateRenderer,
			@Qualifier("testGenerationOutputSchemaProvider") SchemaProvider schemaProvider,
			@Value("${kg.testgen.defaults.prompt-template-id}") String defaultTemplateId) {
		super();
		this.templateLoader = templateLoader;
		this.templateRenderer = templateRenderer;
		this.schemaProvider = schemaProvider;
		this.defaultTemplateId = defaultTemplateId;
	}

	public String buildPrompt(Test test, List<DocumentChunk> chunks) {
		return buildPrompt(test, chunks, List.of());
	}

	/**
	 * Builds the generation prompt. When {@code avoidQuestionTexts} is non-empty
	 * (regeneration), an instruction listing existing questions to avoid is
	 * appended.
	 */
	public String buildPrompt(Test test, List<DocumentChunk> chunks, List<String> avoidQuestionTexts) {
		String prompt = renderBasePrompt(test, chunks);
		if (avoidQuestionTexts == null || avoidQuestionTexts.isEmpty()) {
			return prompt;
		}
		String avoidList = avoidQuestionTexts.stream().filter(t -> t != null && !t.isBlank())
				.map(t -> "- " + t.trim()).collect(Collectors.joining("\n"));
		if (avoidList.isEmpty()) {
			return prompt;
		}
		return prompt + "\n\nThe test already contains the questions below. Do NOT repeat or paraphrase any of them;"
				+ " ask about a different fact from the context:\n" + avoidList;
	}

	private String renderBasePrompt(Test test, List<DocumentChunk> chunks) {
		Objects.requireNonNull(test, "Test must not be null.");
		Objects.requireNonNull(chunks, "Chunks must not be null.");

		String templateId = resolveTemplateId(test);
		String template = templateLoader.loadTemplate(templateId);

		Map<String, Object> vars = new LinkedHashMap<>();

		// --- Test / generation params (adapt names to your Test model) ---
		vars.put("difficulty", safe(test.getDifficulty()));
		vars.put("questionCount", test.getQuestionCount());
		vars.put("language", safe(test.getLanguage()));
		vars.put("minMulti", safe(test.getMinMultipleCorrectQuestionsCount()));

		// --- The generating model (more below) ---
		vars.put("model", safe(test.getGenerationModel()));

		// --- Grounding context from retrieved chunks ---
		vars.put("context", buildContextBlock(chunks));
		vars.put("outputSchema", schemaProvider.getOutputSchemaJson());

		return templateRenderer.render(template, vars);
	}

	/**
	 * @return the template id actually used for the test (including the
	 *         cardinality suffix)
	 */
	public String resolveTemplateId(Test test) {
		// Option A: store on Test as templateId; otherwise default.
		String templateId = test.getPromptTemplateId();
		templateId = (templateId == null || templateId.isBlank()) ? defaultTemplateId : templateId.trim();

		// Add suffix based on answer cardinality.
		if (test.getAnswerCardinality() == AnswerCardinality.MULTIPLE_CORRECT) {
			templateId = templateId + "-multiple-correct";
		}

		return templateId;
	}

	private String buildContextBlock(List<DocumentChunk> chunks) {
		// Keep it deterministic and readable for the LLM:
		// - preserve order as provided (assume already ranked)
		// - include chunk metadata if you have it (doc/page/section)
		return chunks.stream().map(this::formatChunk).collect(Collectors.joining("\n\n---\n\n"));
	}

	private String formatChunk(DocumentChunk e) {
		// Adjust to your ChunkEmbedding model fields.
		// The goal: “source + text”
		String source = "";
		if (e.getTopicId() != null)
			source += "topicId=" + e.getTopicId() + " ";
		if (e.getDocumentId() != null)
			source += "docId=" + e.getDocumentId() + " ";
		if (e.getId() != null)
			source += "chunkId=" + e.getId() + " ";

		String header = source.isBlank() ? "" : ("[SOURCE: " + source.trim() + "]\n");
		return header + safe(e.getChunkText());
	}

	private String safe(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

}
