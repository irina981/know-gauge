package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "The editable details of a test (allowed until it is published)")
public record TestDetailsInput(
		@Schema(description = "Test name (required, max 200 characters)", example = "Fire safety at the Corvane offices", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 200) String name,
		@Schema(description = "Optional description (max 2000 characters); blank removes it", example = "Short test for new employees") @Size(max = 2000) String description) {
}
