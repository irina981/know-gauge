package com.knowgauge.core.service.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.ModelTokenUsage;
import com.knowgauge.core.model.TokenPrice;

/** Prices token usage with the configured token prices. */
public final class TokenPricing {

	private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

	/**
	 * @param costUsd        what the priced models cost, in US dollars (6 decimals)
	 * @param unpricedModels models with tokens but no price; their tokens are not in {@code costUsd}
	 */
	public record Cost(BigDecimal costUsd, List<String> unpricedModels) {
	}

	private TokenPricing() {
	}

	public static Cost cost(List<ModelTokenUsage> usage, List<TokenPrice> prices) {
		BigDecimal total = BigDecimal.ZERO;
		List<String> unpriced = new ArrayList<>();
		for (ModelTokenUsage u : usage) {
			if (u.inputTokens() == 0 && u.outputTokens() == 0) {
				continue;
			}
			Optional<TokenPrice> price = priceFor(u.model(), prices);
			if (price.isEmpty()) {
				unpriced.add(u.model() == null ? "unknown" : u.model());
				continue;
			}
			total = total.add(BigDecimal.valueOf(u.inputTokens()).multiply(price.get().getInputUsdPerMillion()))
					.add(BigDecimal.valueOf(u.outputTokens()).multiply(price.get().getOutputUsdPerMillion()));
		}
		return new Cost(total.divide(MILLION, 6, RoundingMode.HALF_UP), unpriced);
	}

	/** The price whose model is the model name itself or its longest prefix, ignoring case. */
	static Optional<TokenPrice> priceFor(String model, List<TokenPrice> prices) {
		if (model == null) {
			return Optional.empty();
		}
		String name = model.toLowerCase();
		return prices.stream().filter(p -> p.getModel() != null && name.startsWith(p.getModel().toLowerCase()))
				.max(Comparator.comparingInt(p -> p.getModel().length()));
	}
}
