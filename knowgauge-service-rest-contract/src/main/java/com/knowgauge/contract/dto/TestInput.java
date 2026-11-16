package com.knowgauge.contract.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Input used to generate a new test")
public record TestInput(
		@Schema(description = "Test name (required, max 200 characters). Clients supply a default when the user leaves it empty", example = "Java Concurrency – Medium (10 questions)", requiredMode = Schema.RequiredMode.REQUIRED) String name,
		@Schema(description = "Optional free-text description (max 2000 characters)", example = "Practice test for the threads and executors chapters") String description,
		@Schema(description = "Topics to scope retrieval and generation", example = "[101, 102]") List<Long> topicIds,
		@Schema(description = "Optional explicit document scope", example = "[2001, 2002]") List<Long> documentIds,
		@Schema(description = "Desired difficulty", example = "MEDIUM") String difficulty,
		@Schema(description = "Avoid repeating already-used questions", example = "true") Boolean avoidRepeats,
		@Schema(description = "Coverage strategy: BALANCED_PER_DOC_CHUNKS (default), BALANCED_PER_DOCS or FOCUSED", example = "BALANCED_PER_DOC_CHUNKS") String coverageMode,
		@Schema(description = "Required for FOCUSED (max 2000 characters): what the questions should be about. The chunks most similar to it are used; ignored for the other modes", example = "Thread pools and how to shut down an ExecutorService") String focusQuery,
		@Schema(description = "Requested number of questions", example = "10") Integer questionCount,
		@Schema(description = "Answer cardinality mode", example = "SINGLE_CORRECT") String answerCardinality,
		@Schema(description = "Output language", example = "en") String language,
		@Schema(description = "Model override for this test", example = "gpt-4o-mini") String generationModel,
		@Schema(description = "Prompt template id", example = "mcq-default") String promptTemplateId,
		@Schema(description = "Provider-specific generation parameters", example = "{\"temperature\":0.2,\"topP\":1.0}") Map<String, Object> generationParams) {
}
