package com.knowgauge.contract.dto;

import java.time.Instant;

public record DocumentDto(

        Long id,

        Long topicId,

        String title,

        String originalFileName,

        String contentType,

        long fileSizeBytes,

        String storageKey,     // MinIO key

        String etag,          // storage fingerprint

        String version,     // nullable if versioning disabled

        Instant createdAt   // useful for UI & auditing
) {}

