package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Metadata for uploaded document")
public record DocumentInput(
		@NotEmpty @Schema(description = "Owning topic id", example = "101") Long topicId,
		@Schema(description = "Human-readable document title", example = "Spring Boot Fundamentals") String title,
		@Schema(description = "Original uploaded file name", example = "spring-boot-fundamentals.pdf") String originalFileName,
		@Schema(description = "MIME type", example = "application/pdf") String contentType,
		@Schema(description = "File size in bytes", example = "245760") long fileSizeBytes) {
}
