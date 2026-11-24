package com.knowgauge.contract.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Schema(description = "The price of a model, in US dollars per million tokens")
public record TokenPriceInput(
		@NotNull @DecimalMin("0") @Digits(integer = 6, fraction = 6) BigDecimal inputUsdPerMillion,
		@NotNull @DecimalMin("0") @Digits(integer = 6, fraction = 6) BigDecimal outputUsdPerMillion) {
}
