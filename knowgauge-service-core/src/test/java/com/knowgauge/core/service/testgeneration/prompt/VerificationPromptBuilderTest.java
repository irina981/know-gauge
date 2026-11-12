package com.knowgauge.core.service.testgeneration.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.support.QuestionFixtures;

class VerificationPromptBuilderTest {

	private final VerificationPromptBuilder builder = new VerificationPromptBuilder(new PromptTemplateLoader() {
		@Override
		public String loadTemplate(String templateId) {
			return "{{input}}";
		}

		@Override
		public boolean exists(String templateId) {
			return true;
		}
	}, (template, vars) -> template.replace("{{input}}", String.valueOf(vars.get("input"))), () -> "{}",
			"verification");

	@Test
	void sendsCitedSourcesOnceAndOnlyTheDistractors() throws Exception {
		TestQuestionVersion first = QuestionFixtures.version("Which map keeps insertion order?", Set.of(OptionLetter.A),
				"LinkedHashMap", "HashMap", "TreeMap", "IdentityHashMap");
		first.setSourceChunkIds(List.of(11L, 12L));
		TestQuestionVersion second = QuestionFixtures.version("Which map sorts its keys?", Set.of(OptionLetter.C),
				"LinkedHashMap", "HashMap", "TreeMap", "IdentityHashMap");
		second.setSourceChunkIds(List.of(12L, 99L));
		List<TestQuestion> questions = List.of(QuestionFixtures.draft(first), QuestionFixtures.draft(second));
		List<DocumentChunk> chunks = List.of(chunk(11L, "LinkedHashMap keeps insertion order."),
				chunk(12L, "TreeMap sorts its keys."), chunk(13L, "Not cited."));

		String prompt = builder.buildPrompt(questions,
				QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT, TestStatus.CREATED), chunks);

		JsonNode input = new ObjectMapper().readTree(prompt);
		assertThat(input.get("sources")).hasSize(2);
		assertThat(input.get("sources").get(0).get("chunkId").asLong()).isEqualTo(11L);
		assertThat(input.get("sources").get(1).get("text").asText()).isEqualTo("TreeMap sorts its keys.");
		JsonNode q1 = input.get("questions").get(0);
		assertThat(q1.get("sourceChunkIds").toString()).isEqualTo("[11,12]");
		assertThat(new ObjectMapper().convertValue(q1.get("answers"), Map.class)).containsOnlyKeys("B", "C", "D");
		assertThat(q1.get("correctAnswers").toString()).isEqualTo("[\"LinkedHashMap\"]");
		assertThat(input.get("questions").get(1).get("answers").has("C")).isFalse();
	}

	@Test
	void worksWithoutChunks() throws Exception {
		List<TestQuestion> questions = List.of(QuestionFixtures.draft(QuestionFixtures.defaultVersion()));

		String prompt = builder.buildPrompt(questions,
				QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT, TestStatus.CREATED), null);

		JsonNode input = new ObjectMapper().readTree(prompt);
		assertThat(input.get("sources")).isEmpty();
		assertThat(input.get("questions")).hasSize(1);
	}

	private static DocumentChunk chunk(long id, String text) {
		DocumentChunk chunk = DocumentChunk.builder().chunkText(text).build();
		chunk.setId(id);
		return chunk;
	}
}
