package com.knowgauge.contract.dto;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A question as shown in an attempt. The answer key and explanation are only included once the attempt is scored")
public record AttemptQuestionDto(
		@Schema(description = "Question slot id; use it when submitting answers", example = "7001") Long questionId,
		@Schema(description = "Question version shown in this attempt", example = "9001") Long questionVersionId,
		@Schema(description = "0-based position in the test", example = "0") Integer questionIndex,
		@Schema(description = "Question text") String questionText,
		@Schema(description = "Option texts by letter (A-D)") Map<String, String> options,
		@Schema(description = "Options the user chose (after scoring)", example = "[\"A\"]") List<String> chosenOptions,
		@Schema(description = "Correct options (after scoring)", example = "[\"A\"]") List<String> correctOptions,
		@Schema(description = "Whether the answer was correct (after scoring)") Boolean correct,
		@Schema(description = "Why the answer is correct (after scoring)") String explanation) {
}
