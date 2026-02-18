package com.knowgauge.contract.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Document metadata")
public record DocumentDto(

        @Schema(description = "Document id", example = "2001") Long id,

        @Schema(description = "Owning topic id", example = "101") Long topicId,

        @Schema(description = "Document title", example = "Spring Boot Fundamentals") String title,

        @Schema(description = "Original uploaded file name", example = "spring-boot-fundamentals.pdf") String originalFileName,

        @Schema(description = "MIME type", example = "application/pdf") String contentType,

        @Schema(description = "File size in bytes", example = "245760") long fileSizeBytes,

        @Schema(description = "Object storage key", example = "docs/2026/02/18/2001.pdf") String storageKey,     // MinIO key

        @Schema(description = "Storage ETag", example = "a1b2c3d4") String etag,          // storage fingerprint

        @Schema(description = "Object version id, if versioning is enabled", example = "3Lgk9J") String version,     // nullable if versioning disabled

        @Schema(description = "Creation time (UTC)", example = "2026-02-18T10:15:30Z") Instant createdAt   // useful for UI & auditing
) {}

