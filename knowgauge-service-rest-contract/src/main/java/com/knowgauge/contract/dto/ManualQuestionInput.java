package com.knowgauge.contract.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "A question written by the reviewer to replace the current one")
public record ManualQuestionInput(
		@NotNull(message = "expectedCurrentVersionId is required") @Schema(description = "Version the reviewer looked at. The action fails with 409 if the question changed since", example = "12001") Long expectedCurrentVersionId,
		@NotBlank(message = "questionText is required") @Schema(description = "Question text", example = "Which statement about the JVM heap is correct?") String questionText,
		@NotNull(message = "options are required") @Size(min = 4, max = 4, message = "exactly 4 options (A-D) are required") @Schema(description = "Options A-D") Map<String, @Valid ManualOptionInput> options,
		@Schema(description = "Optional explanation") String explanation,
		@Size(max = 2000) @Schema(description = "Optional review comment, kept in the history") String comment) {

	@Schema(description = "One option of a manually written question")
	public record ManualOptionInput(
			@NotBlank(message = "option text is required") @Schema(description = "Option text") String text,
			@NotNull(message = "option correct flag is required") @Schema(description = "Whether the option is correct") Boolean correct) {
	}
}
