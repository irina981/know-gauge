package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional body of the publish and unpublish actions")
public record PublishTestInput(
		@Size(max = 2000) @Schema(description = "Optional comment, kept in the history", example = "Reviewed and approved for the Q1 onboarding cohort") String comment) {
}
