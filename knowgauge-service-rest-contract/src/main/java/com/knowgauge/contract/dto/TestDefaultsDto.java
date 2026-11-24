package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a new test of the current workspace gets for each field its request leaves empty: the workspace's own configuration, else the system values (ADR014)")
public record TestDefaultsDto(
		@Schema(description = "Number of questions", example = "10") Integer questionCount,
		@Schema(description = "EASY, MEDIUM or HARD", example = "MEDIUM") String difficulty,
		@Schema(description = "SINGLE_CORRECT or MULTIPLE_CORRECT", example = "SINGLE_CORRECT") String answerCardinality,
		@Schema(description = "Coverage strategy", example = "BALANCED_PER_DOC_CHUNKS") String coverageMode,
		@Schema(description = "Language of the questions", example = "EN") String language,
		@Schema(description = "Model that writes the questions", example = "gpt-4.1-mini") String generationModel) {
}
