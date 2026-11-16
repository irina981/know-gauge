package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What a participant sees before starting a test from a link")
public record PublicTestDto(
		@Schema(description = "Test name", example = "Health & Safety basics") String name,
		@Schema(description = "Optional description") String description,
		@Schema(description = "Number of questions", example = "10") Integer questionCount,
		@Schema(description = "SINGLE_CORRECT or MULTIPLE_CORRECT", example = "SINGLE_CORRECT") String answerCardinality) {
}
