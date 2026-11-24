package com.knowgauge.contract.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A model's prices as a price catalog lists them, not saved. Third-party data: check before saving.")
public record TokenPriceSuggestionDto(
		String model,
		@Schema(description = "False when the catalog does not list the model; the prices are then null") boolean known,
		@Schema(description = "US dollars per million input tokens") BigDecimal inputUsdPerMillion,
		@Schema(description = "US dollars per million output tokens") BigDecimal outputUsdPerMillion,
		@Schema(description = "The catalog entry the prices come from, e.g. openai/gpt-4.1") String catalogModel,
		@Schema(description = "The provider the catalog names for the entry, e.g. openai") String provider,
		@Schema(description = "The catalog, e.g. LiteLLM price catalog") String source) {
}
