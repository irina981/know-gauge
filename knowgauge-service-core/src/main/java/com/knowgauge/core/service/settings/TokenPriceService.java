package com.knowgauge.core.service.settings;

import java.math.BigDecimal;
import java.util.List;

import com.knowgauge.core.model.TokenPrice;
import com.knowgauge.core.model.TokenPriceSuggestion;

/** System-wide LLM token prices, kept by an {@code ADMIN} on the System settings page. */
public interface TokenPriceService {

	/** @return every price, by model name */
	List<TokenPrice> list();

	/**
	 * Sets the price of a model, adding it when it is new.
	 *
	 * @throws IllegalArgumentException for a blank or too long model name, or a missing or negative price
	 */
	TokenPrice save(String model, BigDecimal inputUsdPerMillion, BigDecimal outputUsdPerMillion);

	/**
	 * Model names to offer when adding a price: the configured generation and verification models and every model
	 * recorded for an LLM call, by name.
	 */
	List<String> knownModels();

	/** OpenAI chat models listed in the price catalog, by name; empty when the catalog cannot be read. */
	List<String> catalogModels();

	/** @throws com.knowgauge.core.exception.ResourceNotFoundException when the model has no price */
	void delete(String model);

	/**
	 * The model's prices in the price catalog, not saved. Outside any transaction: it may download the catalog.
	 *
	 * @throws IllegalArgumentException for a blank model name
	 */
	TokenPriceSuggestion suggest(String model);
}
