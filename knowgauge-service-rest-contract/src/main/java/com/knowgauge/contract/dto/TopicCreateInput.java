package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload used to create a topic")
public record TopicCreateInput(
		@NotBlank @Schema(description = "Topic display name", example = "Databases") String name,
		@Schema(description = "Optional topic description", example = "Relational and non-relational database concepts") String description,
		@Schema(description = "Parent topic id for nested topics", example = "10") Long parentId,
		@Schema(description = "For a root topic (no parent): the shared library to create it in instead of the own one. ADMIN only (ADR015).", example = "1") Long libraryTenantId) {
}
