package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One answer option of a question version")
public record TestQuestionOptionDto(
		@Schema(description = "Option letter (A, B, C, or D)", example = "A") String letter,
		@Schema(description = "Option text") String text,
		@Schema(description = "Whether the option is part of the answer key", example = "false") boolean correct,
		@Schema(description = "Whether the verifier flagged this distractor as possibly true", example = "true") boolean flagged,
		@Schema(description = "Verifier's clearly false replacement (only when flagged)") String suggestedText,
		@Schema(description = "Verifier's reason for the flag (only when flagged)") String flagReason) {
}
