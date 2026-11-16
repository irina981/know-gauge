package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An attempt with its test and questions")
public record AttemptDetailDto(
		@Schema(description = "The attempt and its score") AttemptDto attempt,
		@Schema(description = "Name of the test", example = "Java Concurrency – Medium (10 questions)") String testName,
		@Schema(description = "SINGLE_CORRECT or MULTIPLE_CORRECT: how many options may be chosen", example = "SINGLE_CORRECT") String answerCardinality,
		@Schema(description = "Questions in order") List<AttemptQuestionDto> questions) {
}
