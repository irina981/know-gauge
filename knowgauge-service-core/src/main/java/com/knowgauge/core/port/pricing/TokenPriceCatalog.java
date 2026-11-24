package com.knowgauge.core.port.pricing;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.EmbeddingModelPrice;
import com.knowgauge.core.model.TokenPriceSuggestion;

/** A published list of LLM token prices, to suggest a model's price to an admin. */
public interface TokenPriceCatalog {

	/**
	 * @return the model's prices, or a suggestion with {@code known = false} when the catalog does not list it
	 * @throws com.knowgauge.core.exception.PriceCatalogUnavailableException when the catalog cannot be read
	 */
	TokenPriceSuggestion lookUp(String model);

	/**
	 * @param provider e.g. {@code openai}
	 * @return the names of the provider's chat models in the catalog, by name; empty when the catalog cannot be read
	 */
	List<String> chatModels(String provider);

	/** @return the embedding model's catalog entry; empty when it is not listed or the catalog cannot be read */
	Optional<EmbeddingModelPrice> embeddingModel(String model);
}
