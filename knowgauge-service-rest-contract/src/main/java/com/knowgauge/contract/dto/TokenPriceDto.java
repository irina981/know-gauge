package com.knowgauge.contract.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What one LLM model costs (system setting). The model name matches recorded calls exactly or as a prefix; the longest match wins.")
public record TokenPriceDto(
		@Schema(description = "Model name or prefix, e.g. gpt-4.1") String model,
		@Schema(description = "US dollars per million input tokens") BigDecimal inputUsdPerMillion,
		@Schema(description = "US dollars per million output tokens") BigDecimal outputUsdPerMillion,
		@Schema(description = "Last change") Instant updatedAt) {
}
