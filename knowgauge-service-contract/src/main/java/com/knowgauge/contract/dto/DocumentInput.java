package com.knowgauge.contract.dto;

import jakarta.validation.constraints.NotEmpty;

public record DocumentInput(@NotEmpty Long topicId, String title, String originalFileName, String contentType,
		long fileSizeBytes) {
}
