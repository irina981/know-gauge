package com.knowgauge.contract.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An attempt at a published test, with its score once submitted")
public record AttemptDto(
		@Schema(description = "Attempt id", example = "42") Long id,
		@Schema(description = "Test that was taken", example = "5001") Long testId,
		@Schema(description = "Administrator who took the test; empty for participants", example = "1") Long userId,
		@Schema(description = "Full name the participant entered", example = "Ana Petrovic") String participantName,
		@Schema(description = "Participant email (lower-cased); from the invitation for personal links", example = "ana@example.com") String participantEmail,
		@Schema(description = "Personal invitation the attempt was started from, if any", example = "12") Long invitationId,
		@Schema(description = "STARTED, SUBMITTED or SCORED", example = "SCORED") String status,
		@Schema(description = "Number of questions in the test", example = "10") Integer totalQuestions,
		@Schema(description = "Correctly answered questions (0 until scored)", example = "8") Integer correctCount,
		@Schema(description = "Wrong or unanswered questions (0 until scored)", example = "2") Integer wrongCount,
		@Schema(description = "Score in percent, one decimal (0 until scored)", example = "80.0") Double scorePercent,
		@Schema(description = "When the attempt was started (UTC)") Instant startedAt,
		@Schema(description = "When the answers were submitted (UTC)") Instant submittedAt,
		@Schema(description = "When the attempt was scored (UTC)") Instant scoredAt) {
}
