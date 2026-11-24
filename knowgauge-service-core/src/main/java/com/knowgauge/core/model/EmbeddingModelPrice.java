package com.knowgauge.core.model;

import java.math.BigDecimal;

/**
 * An embedding model as a price catalog lists it.
 *
 * @param inputUsdPerMillion US dollars per million input tokens (embeddings have no output tokens)
 * @param vectorSize         the length of its vectors, when the catalog says; {@code null} otherwise
 */
public record EmbeddingModelPrice(String catalogModel, BigDecimal inputUsdPerMillion, Integer vectorSize) {
}
