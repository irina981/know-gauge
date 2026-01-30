package com.knowgauge.core.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

	private Long id;

	private Long documentId;

	private Long topicId;

	private Long sectionId;

	private Integer chunkIndex;

	private String chunkText;

	private Integer startPage;

	private Integer endPage;

	private Integer charStart;

	private Integer charEnd;

	private String checksum;

	private Instant createdAt;

}
