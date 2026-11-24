package com.knowgauge.contract.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The embedding model (read-only: changing it means re-ingesting every document)")
public record EmbeddingInfoDto(
		@Schema(description = "kg.embedding.openai.model") String model,
		@Schema(description = "kg.embedding.dimension, also the vector column size") int dimension,
		@Schema(description = "The price catalog entry, if listed") String catalogModel,
		@Schema(description = "US dollars per million tokens, from the price catalog; null when unknown") BigDecimal inputUsdPerMillion,
		@Schema(description = "Vector size the price catalog gives; null when unknown") Integer catalogVectorSize,
		@Schema(description = "Stored embeddings per model; only those of the current model are used") List<EmbeddingModelCountDto> byModel) {

	public record EmbeddingModelCountDto(String model, long embeddings, long documents) {
	}
}
