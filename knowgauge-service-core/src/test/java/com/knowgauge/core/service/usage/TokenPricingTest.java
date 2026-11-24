package com.knowgauge.core.service.usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.knowgauge.core.model.ModelTokenUsage;
import com.knowgauge.core.model.TokenPrice;

class TokenPricingTest {

	private static TokenPrice price(String model, String input, String output) {
		return TokenPrice.builder().model(model).inputUsdPerMillion(new BigDecimal(input))
				.outputUsdPerMillion(new BigDecimal(output)).build();
	}

	private final List<TokenPrice> prices = List.of(price("gpt-4.1", "2", "8"), price("gpt-4.1-mini", "0.4", "1.6"));

	@Test
	void aDatedModelNameUsesTheLongestMatchingPrefix() {
		assertThat(TokenPricing.priceFor("gpt-4.1-2025-04-14", prices)).get().extracting(TokenPrice::getModel)
				.isEqualTo("gpt-4.1");
		assertThat(TokenPricing.priceFor("gpt-4.1-mini-2025-04-14", prices)).get().extracting(TokenPrice::getModel)
				.isEqualTo("gpt-4.1-mini");
		assertThat(TokenPricing.priceFor("GPT-4.1", prices)).isPresent();
		assertThat(TokenPricing.priceFor("gpt-5", prices)).isEmpty();
	}

	@Test
	void costAddsInputAndOutputPerMillionTokens() {
		TokenPricing.Cost cost = TokenPricing.cost(List.of(new ModelTokenUsage("gpt-4.1-2025-04-14", 500_000, 100_000),
				new ModelTokenUsage("gpt-4.1-mini", 1_000_000, 0)), prices);

		// 0.5 M × $2 + 0.1 M × $8 + 1 M × $0.40
		assertThat(cost.costUsd()).isEqualByComparingTo("2.2");
		assertThat(cost.unpricedModels()).isEmpty();
	}

	@Test
	void modelsWithoutAPriceAreListedAndLeftOut() {
		TokenPricing.Cost cost = TokenPricing.cost(List.of(new ModelTokenUsage("gpt-4.1", 1_000_000, 0),
				new ModelTokenUsage("gpt-5.6-luna", 10, 10), new ModelTokenUsage("unused", 0, 0)), prices);

		assertThat(cost.costUsd()).isEqualByComparingTo("2");
		assertThat(cost.unpricedModels()).containsExactly("gpt-5.6-luna");
	}
}
