package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Body of the accept-fix and regenerate review actions")
public record ReviewActionInput(
		@NotNull(message = "expectedCurrentVersionId is required") @Schema(description = "Version the reviewer looked at. The action fails with 409 if the question changed since", example = "12001") Long expectedCurrentVersionId,
		@Size(max = 2000) @Schema(description = "Optional review comment, kept in the history") String comment) {
}
