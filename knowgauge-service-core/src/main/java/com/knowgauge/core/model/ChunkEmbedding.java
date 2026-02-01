package com.knowgauge.core.model;

import java.time.Instant;

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

	private Long chunkId;

	private float[] embedding;

	private String embeddingModel;

	private Instant createdAt;

}
