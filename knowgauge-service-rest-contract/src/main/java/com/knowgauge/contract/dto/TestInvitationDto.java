package com.knowgauge.contract.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A personal invitation and what came of it")
public record TestInvitationDto(
		@Schema(description = "Invitation id", example = "12") Long id,
		@Schema(description = "Test", example = "5001") Long testId,
		@Schema(description = "Invited address (lower-cased)", example = "ana@example.com") String email,
		@Schema(description = "The personal link") String link,
		@Schema(description = "When it was invited (UTC)") Instant createdAt,
		@Schema(description = "Last successful send (UTC); empty if never delivered") Instant sentAt,
		@Schema(description = "How often a send was attempted", example = "1") Integer sendCount,
		@Schema(description = "Error of the last send; empty after a successful one") String sendError,
		@Schema(description = "Attempt started from the link; empty while unused", example = "42") Long attemptId,
		@Schema(description = "STARTED or SCORED; empty while unused", example = "SCORED") String attemptStatus,
		@Schema(description = "Name the participant entered", example = "Ana Petrovic") String participantName,
		@Schema(description = "Score in percent once scored", example = "80.0") Double scorePercent) {
}
