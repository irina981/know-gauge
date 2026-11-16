package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A participant's attempt. Once scored it has the score and the participant's own answers marked right or wrong; the correct options and explanations are never included")
public record PublicAttemptDto(
		@Schema(description = "Token that addresses this attempt") String attemptToken,
		@Schema(description = "Test name", example = "Health & Safety basics") String testName,
		@Schema(description = "SINGLE_CORRECT or MULTIPLE_CORRECT", example = "SINGLE_CORRECT") String answerCardinality,
		@Schema(description = "Full name of the participant", example = "Ana Petrovic") String participantName,
		@Schema(description = "STARTED or SCORED", example = "STARTED") String status,
		@Schema(description = "Number of questions", example = "10") Integer totalQuestions,
		@Schema(description = "Correct answers (after scoring)", example = "8") Integer correctCount,
		@Schema(description = "Score in percent (after scoring)", example = "80.0") Double scorePercent,
		@Schema(description = "Questions to answer; once scored, with chosenOptions and correct (correctOptions and explanation stay empty)") List<AttemptQuestionDto> questions) {
}
