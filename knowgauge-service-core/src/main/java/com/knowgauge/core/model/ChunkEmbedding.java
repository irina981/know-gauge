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
public class ChunkEmbedding {

	private Long id;

	private Long chunkId;

	private float[] embedding;

	private String embeddingModel;

	private Instant createdAt;

}
