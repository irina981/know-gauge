package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated question belonging to a test")
public record TestQuestionDto(
		@Schema(description = "Question identifier", example = "9001") Long id,
		@Schema(description = "Owning test identifier", example = "5001") Long testId,
		@Schema(description = "Question order within test", example = "1") Integer questionIndex,
		@Schema(description = "Question text") String questionText,
		@Schema(description = "Option A") String optionA,
		@Schema(description = "Option B") String optionB,
		@Schema(description = "Option C") String optionC,
		@Schema(description = "Option D") String optionD,
		@Schema(description = "Correct options", example = "[\"A\",\"C\"]") List<String> correctOptions,
		@Schema(description = "Short explanation for the correct answer") String explanation,
		@Schema(description = "Chunk ids used as grounding sources", example = "[10101, 10102]") List<Long> sourceChunkIdsJson) {
}
