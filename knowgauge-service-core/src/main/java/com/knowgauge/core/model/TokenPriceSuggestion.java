package com.knowgauge.core.model;

import java.math.BigDecimal;

/**
 * Prices a price catalog lists for a model, for an admin to check before saving. {@code known} is false when the
 * catalog does not list the model; the prices are then {@code null}.
 *
 * @param catalogModel the catalog entry the prices come from, e.g. {@code gpt-4.1} or {@code openai/gpt-4.1}
 * @param source       the catalog, e.g. "LiteLLM price catalog"
 */
public record TokenPriceSuggestion(String model, boolean known, BigDecimal inputUsdPerMillion,
		BigDecimal outputUsdPerMillion, String catalogModel, String provider, String source) {
}
