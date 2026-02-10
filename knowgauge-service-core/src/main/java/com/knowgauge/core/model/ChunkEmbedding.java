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
public class ChunkEmbedding extends AuditableObject {
	
	private Long tenantId;

	private Long topicId;
	
	private Long documentId;
	
	private Integer documentVersion;

	private Long sectionId;

	private Long chunkId;

	private float[] embedding;

	private String embeddingModel;
	
	private String chunkChecksum;

}
