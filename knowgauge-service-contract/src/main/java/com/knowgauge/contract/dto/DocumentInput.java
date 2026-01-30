package com.knowgauge.contract.dto;

public record DocumentInput(
        Long topicId,
        String title,
        String originalFilename,
        String contentType,
        long fileSizeBytes
) {}
