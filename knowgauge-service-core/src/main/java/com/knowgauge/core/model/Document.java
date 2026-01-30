package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.DocumentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

	private Long id;

	private String title;

	private String originalFileName;

	private String contentType;

	private Long fileSizeBytes;

	private String storageKey;

	private Long topicId;

	private String version;

	private DocumentStatus status;

	private Instant uploadedAt;

	private Instant ingestedAt;

	private String uploadedBy;

	private String errorMessage;

}
