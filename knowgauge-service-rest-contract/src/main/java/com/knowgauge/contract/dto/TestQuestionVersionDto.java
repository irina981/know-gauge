package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Immutable snapshot of a question's content")
public record TestQuestionVersionDto(
		@Schema(description = "Version identifier", example = "12001") Long id,
		@Schema(description = "Question (slot) identifier", example = "9001") Long questionId,
		@Schema(description = "Version number, 1 = as generated", example = "2") Integer versionNo,
		@Schema(description = "GENERATED, REGENERATED, FIX_ACCEPTED or MANUAL", example = "FIX_ACCEPTED") String origin,
		@Schema(description = "Version this one replaced") Long parentVersionId,
		@Schema(description = "LLM call that produced the content (null for FIX_ACCEPTED and MANUAL)") Long generationRunId,
		@Schema(description = "Verification call that checked the content") Long verificationRunId,
		@Schema(description = "Question text") String questionText,
		@Schema(description = "Options mapped by letter (A, B, C, D)") Map<String, TestQuestionOptionDto> options,
		@Schema(description = "Answer key", example = "[\"A\"]") List<String> correctOptions,
		@Schema(description = "Letters of flagged options", example = "[]") List<String> flaggedOptions,
		@Schema(description = "Explanation") String explanation,
		@Schema(description = "Grounding chunk ids") List<Long> sourceChunkIds,
		@Schema(description = "Creation time (UTC)") Instant createdAt,
		@Schema(description = "Author user id; null = system (LLM generation)") Long createdBy) {
}
