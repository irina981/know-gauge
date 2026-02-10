package com.knowgauge.core.model;

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
public class DocumentChunk extends AuditableObject {
	private Long tenantId;

	private Long topicId;
	
	private Long documentId;
	
	private Integer documentVersion;

	private Long sectionId;

	private Integer ordinal;

	private String chunkText;

	private Integer startPage;

	private Integer endPage;

	private Integer charStart;

	private Integer charEnd;

	private String checksum;

}
