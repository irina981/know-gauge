package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Entry of a test's history trail")
public record TestEventDto(
		@Schema(description = "Event identifier") Long id,
		@Schema(description = "Test identifier") Long testId,
		@Schema(description = "Question identifier; null for test-level events") Long questionId,
		@Schema(description = "Event type", example = "FIX_ACCEPTED") String eventType,
		@Schema(description = "Version before the change") Long fromVersionId,
		@Schema(description = "Version after the change") Long toVersionId,
		@Schema(description = "Acting user id; null = system") Long actorUserId,
		@Schema(description = "Reviewer comment") String comment,
		@Schema(description = "Event details (e.g. flagged option, reason, suggestion, run ids)") Map<String, Object> payload,
		@Schema(description = "When it happened (UTC)") Instant createdAt) {
}
