package com.knowgauge.contract.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Input used to generate a new test")
public record TestInput(
		@Schema(description = "Topics to scope retrieval and generation", example = "[101, 102]") List<Long> topicIds,
		@Schema(description = "Optional explicit document scope", example = "[2001, 2002]") List<Long> documentIds,
		@Schema(description = "Desired difficulty", example = "MEDIUM") String difficulty,
		@Schema(description = "Avoid repeating already-used questions", example = "true") Boolean avoidRepeats,
		@Schema(description = "Coverage strategy", example = "BALANCED") String coverageMode,
		@Schema(description = "Requested number of questions", example = "10") Integer questionCount,
		@Schema(description = "Answer cardinality mode", example = "SINGLE_CORRECT") String answerCardinality,
		@Schema(description = "Output language", example = "en") String language,
		@Schema(description = "Model override for this test", example = "gpt-4o-mini") String generationModel,
		@Schema(description = "Prompt template id", example = "mcq-default") String promptTemplateId,
		@Schema(description = "Provider-specific generation parameters", example = "{\"temperature\":0.2,\"topP\":1.0}") Map<String, Object> generationParams) {
}
