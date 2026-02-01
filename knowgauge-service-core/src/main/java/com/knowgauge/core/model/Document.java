package com.knowgauge.core.model;

import java.time.Instant;

import com.knowgauge.core.model.enums.DocumentStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Document extends AuditableObject {

	private String title;

	private String originalFileName;

	private String contentType;

	private Long fileSizeBytes;

	private String storageKey;

	private Long topicId;

	private String version;

	private String etag;

	private DocumentStatus status;

	private String errorMessage;

	private Instant ingestedAt;

}
