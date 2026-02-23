package com.knowgauge.core.service.testgeneration.prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.service.testgeneration.schema.TestQuestionSchemaProvider;

@Component
public class TestPromptBuilder {
	private final PromptTemplateLoader templateLoader;
	private final PromptTemplateRenderer templateRenderer;
	private final TestQuestionSchemaProvider schemaProvider;
	private final String defaultTemplateId;

	public TestPromptBuilder(PromptTemplateLoader templateLoader, PromptTemplateRenderer templateRenderer,
			TestQuestionSchemaProvider schemaProvider,
			@Value("${kg.testgen.defaults.prompt-template-id}") String defaultTemplateId) {
		super();
		this.templateLoader = templateLoader;
		this.templateRenderer = templateRenderer;
		this.schemaProvider = schemaProvider;
		this.defaultTemplateId = defaultTemplateId;
	}

	public String buildPrompt(Test test, List<DocumentChunk> chunks) {
		Objects.requireNonNull(test, "Test must not be null.");
		Objects.requireNonNull(chunks, "Chunks must not be null.");

		String templateId = resolveTemplateId(test);
		String template = templateLoader.loadTemplate(templateId);

		Map<String, Object> vars = new LinkedHashMap<>();

		// --- Test / generation params (adapt names to your Test model) ---
		vars.put("difficulty", safe(test.getDifficulty()));
		vars.put("questionCount", test.getQuestionCount());
		vars.put("language", safe(test.getLanguage()));

		// --- The generating model (more below) ---
		vars.put("model", safe(test.getGenerationModel()));

		// --- Grounding context from retrieved chunks ---
		vars.put("context", buildContextBlock(chunks));
		vars.put("outputSchema", schemaProvider.getOutputSchemaJson());

		return templateRenderer.render(template, vars);
	}

	private String resolveTemplateId(Test test) {
		// Option A: store on Test as templateId; otherwise default.
		String id = test.getPromptTemplateId();
		return (id == null || id.isBlank()) ? defaultTemplateId : id.trim();
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
			source += "topicId=" + e.getDocumentId() + " ";
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
