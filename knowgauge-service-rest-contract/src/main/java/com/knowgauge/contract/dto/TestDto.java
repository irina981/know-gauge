package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated test details returned by API")
public record TestDto(
		@Schema(description = "Test identifier", example = "5001") Long id,
		@Schema(description = "Owning tenant identifier", example = "1") Long tenantId,
		@Schema(description = "Topics used for generation", example = "[101, 102]") List<Long> topicIds,
		@Schema(description = "Documents used for generation", example = "[2001, 2002]") List<Long> documentIds,
		@Schema(description = "Configured difficulty", example = "MEDIUM") String difficulty,
		@Schema(description = "Whether repeats were avoided", example = "true") boolean avoidRepeats,
		@Schema(description = "Coverage strategy", example = "BALANCED") String coverageMode,
		@Schema(description = "Requested number of questions", example = "10") Integer questionCount,
		@Schema(description = "Generation status", example = "COMPLETED") String status,
		@Schema(description = "Generation model used", example = "gpt-4o-mini") String generationModel,
		@Schema(description = "Prompt template id", example = "mcq-default") String promptTemplateId,
		@Schema(description = "Generation parameters used", example = "{\"temperature\":0.2,\"topP\":1.0}") Map<String, Object> generationParams,
		@Schema(description = "Generation start time (UTC)", example = "2026-02-18T10:15:30Z") Instant generationStartedAt,
		@Schema(description = "Generation finish time (UTC)", example = "2026-02-18T10:15:45Z") Instant generationFinishedAt,
		@Schema(description = "Generation failure time (UTC)", example = "2026-02-18T10:15:45Z") Instant generationFailedAt,
		@Schema(description = "Failure reason when status is FAILED", example = "Model request timed out") String generationErrorMessage) {
}
