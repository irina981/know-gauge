package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Body of the dismiss-flags review action (keep the question as it is)")
public record DismissFlagsInput(
		@NotNull(message = "expectedCurrentVersionId is required") @Schema(description = "Version the reviewer looked at. The action fails with 409 if the question changed since", example = "12001") Long expectedCurrentVersionId,
		@NotBlank(message = "A comment is required: say why the verifier's flags are wrong") @Size(max = 2000) @Schema(description = "Why the flags are dismissed, kept in the history", example = "The flagged answer is not a disadvantage of setter injection") String comment) {
}
