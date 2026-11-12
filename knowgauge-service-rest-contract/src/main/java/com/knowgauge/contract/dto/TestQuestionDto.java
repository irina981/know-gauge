package com.knowgauge.contract.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A question of a test, showing its current version")
public record TestQuestionDto(
		@Schema(description = "Question (slot) identifier", example = "9001") Long id,
		@Schema(description = "Owning test identifier", example = "5001") Long testId,
		@Schema(description = "Question order within test", example = "0") Integer questionIndex,
		@Schema(description = "Review status: OK or NEEDS_REVIEW", example = "NEEDS_REVIEW") String status,
		@Schema(description = "Id of the current version. Send it as expectedCurrentVersionId in review actions", example = "12001") Long currentVersionId,
		@Schema(description = "Number of the current version (1 = as generated)", example = "1") Integer versionNo,
		@Schema(description = "How the current version was created: GENERATED, REGENERATED, FIX_ACCEPTED, MANUAL", example = "GENERATED") String origin,
		@Schema(description = "Question text") String questionText,
		@Schema(description = "Options mapped by letter (A, B, C, D)") Map<String, TestQuestionOptionDto> options,
		@Schema(description = "Answer key", example = "[\"A\",\"C\"]") List<String> correctOptions,
		@Schema(description = "Letters of options flagged by the verifier", example = "[\"B\"]") List<String> flaggedOptions,
		@Schema(description = "Short explanation for the correct answer") String explanation,
		@Schema(description = "Chunk ids used as grounding sources", example = "[10101, 10102]") List<Long> sourceChunkIds) {
}
