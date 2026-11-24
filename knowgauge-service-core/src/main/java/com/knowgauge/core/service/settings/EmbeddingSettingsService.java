package com.knowgauge.core.service.settings;

import java.math.BigDecimal;
import java.util.List;

import com.knowgauge.core.model.EmbeddingModelCount;

/**
 * The embedding model, shown read-only on the System settings page. It is not an override like the other settings:
 * every stored vector belongs to the model that made it and every vector query filters by model, so a change means
 * re-ingesting all documents (and the vector size, 1536, must fit). It is changed in the properties files.
 */
public interface EmbeddingSettingsService {

	/**
	 * @param model              {@code kg.embedding.openai.model}
	 * @param dimension          {@code kg.embedding.dimension}, also the column size in the vector database
	 * @param inputUsdPerMillion the model's price per million tokens in the price catalog; {@code null} when unknown
	 * @param catalogVectorSize  the vector size the catalog gives for the model; {@code null} when unknown
	 * @param byModel            stored embeddings per model; ones of another model are not used
	 */
	record EmbeddingInfo(String model, int dimension, String catalogModel, BigDecimal inputUsdPerMillion,
			Integer catalogVectorSize, List<EmbeddingModelCount> byModel) {
	}

	EmbeddingInfo info();
}
