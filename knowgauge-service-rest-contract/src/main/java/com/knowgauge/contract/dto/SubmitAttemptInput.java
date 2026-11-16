package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Answers of an attempt. Questions left out count as unanswered (wrong)")
public record SubmitAttemptInput(
		@NotNull @Valid @Schema(description = "Chosen options per question") List<Answer> answers) {

	@Schema(description = "The options chosen for one question")
	public record Answer(
			@NotNull @Schema(description = "Question id from the attempt", example = "7001") Long questionId,
			@NotNull @Schema(description = "Chosen option letters; empty for no answer", example = "[\"A\"]") List<String> chosenOptions) {
	}
}
