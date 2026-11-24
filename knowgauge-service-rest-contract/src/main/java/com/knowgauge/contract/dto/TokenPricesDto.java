package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The token prices, and the model names to offer when adding one")
public record TokenPricesDto(
		@Schema(description = "Every price, by model") List<TokenPriceDto> prices,
		@Schema(description = "Configured models and every model recorded for an LLM call") List<String> knownModels,
		@Schema(description = "OpenAI chat models listed in the price catalog") List<String> catalogModels) {
}
