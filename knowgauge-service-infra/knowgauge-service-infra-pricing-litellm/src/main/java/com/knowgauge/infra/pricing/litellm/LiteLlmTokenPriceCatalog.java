package com.knowgauge.infra.pricing.litellm;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.core.exception.PriceCatalogUnavailableException;
import com.knowgauge.core.model.EmbeddingModelPrice;
import com.knowgauge.core.model.TokenPriceSuggestion;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;

import lombok.extern.slf4j.Slf4j;

/**
 * LiteLLM's public price catalog ({@code model_prices_and_context_window.json}): one entry per model with its price
 * per token, kept up to date by the LiteLLM project for many providers. Third-party data, not the provider's own, so
 * the admin checks it before saving. The whole file is downloaded and searched here (no model name leaves the server)
 * and kept for {@code cache-hours}.
 */
@Slf4j
@Component
public class LiteLlmTokenPriceCatalog implements TokenPriceCatalog {

	static final String SOURCE = "LiteLLM price catalog";
	/** Catalog modes of models that answer prompts (Chat Completions or Responses API). */
	private static final Set<String> CHAT_MODES = Set.of("chat", "responses");
	private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

	private record Cached(JsonNode catalog, Instant fetchedAt) {
	}

	private final ObjectMapper objectMapper = new ObjectMapper()
			.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NORMAL).build();
	private final URI url;
	private final Duration cacheFor;
	private final Clock clock;
	private volatile Cached cached;

	public LiteLlmTokenPriceCatalog(
			@Value("${kg.settings.token-price-catalog.url:https://raw.githubusercontent.com/BerriAI/litellm/main/model_prices_and_context_window.json}") String url,
			@Value("${kg.settings.token-price-catalog.cache-hours:6}") long cacheHours) {
		this.url = URI.create(url);
		this.cacheFor = Duration.ofHours(cacheHours);
		this.clock = Clock.systemUTC();
	}

	@Override
	public TokenPriceSuggestion lookUp(String model) {
		return find(catalog(), model)
				.map(entry -> new TokenPriceSuggestion(model, true, perMillion(entry.getValue(), "input_cost_per_token"),
						perMillion(entry.getValue(), "output_cost_per_token"), entry.getKey(),
						entry.getValue().path("litellm_provider").asText(null), SOURCE))
				.orElseGet(() -> new TokenPriceSuggestion(model, false, null, null, null, null, SOURCE));
	}

	@Override
	public Optional<EmbeddingModelPrice> embeddingModel(String model) {
		try {
			return findEmbedding(catalog(), model);
		} catch (PriceCatalogUnavailableException e) {
			return Optional.empty();
		}
	}

	/** An embedding entry has an input price only; {@code output_vector_size} gives its vector length, when known. */
	static Optional<EmbeddingModelPrice> findEmbedding(JsonNode catalog, String model) {
		return find(catalog, model, entry -> entry.path("input_cost_per_token").isNumber())
				.map(entry -> new EmbeddingModelPrice(entry.getKey(), perMillion(entry.getValue(), "input_cost_per_token"),
						entry.getValue().path("output_vector_size").isNumber()
								? entry.getValue().path("output_vector_size").asInt()
								: null));
	}

	@Override
	public List<String> chatModels(String provider) {
		try {
			return chatModels(catalog(), provider);
		} catch (PriceCatalogUnavailableException e) {
			return List.of();
		}
	}

	/**
	 * The provider's chat models with both prices, by name: plain names only (no {@code provider/} prefix, no
	 * fine-tuned {@code ft:} entries), as the provider's API expects them.
	 */
	static List<String> chatModels(JsonNode catalog, String provider) {
		List<String> models = new ArrayList<>();
		catalog.fields().forEachRemaining(entry -> {
			JsonNode value = entry.getValue();
			String name = entry.getKey();
			if (hasPrices(value) && provider.equalsIgnoreCase(value.path("litellm_provider").asText())
					&& CHAT_MODES.contains(value.path("mode").asText()) && !name.contains("/")
					&& !name.startsWith("ft:")) {
				models.add(name);
			}
		});
		Collections.sort(models);
		return models;
	}

	/**
	 * The catalog entry for a model: the exact name (ignoring case), else a provider-prefixed name such as
	 * {@code openai/gpt-4.1}, OpenAI's first. Only entries with both an input and an output price count.
	 */
	static Optional<Map.Entry<String, JsonNode>> find(JsonNode catalog, String model) {
		return find(catalog, model, LiteLlmTokenPriceCatalog::hasPrices);
	}

	private static Optional<Map.Entry<String, JsonNode>> find(JsonNode catalog, String model,
			Predicate<JsonNode> usable) {
		String name = model.trim().toLowerCase(Locale.ROOT);
		Map.Entry<String, JsonNode> prefixed = null;
		Iterator<Map.Entry<String, JsonNode>> entries = catalog.fields();
		while (entries.hasNext()) {
			Map.Entry<String, JsonNode> entry = entries.next();
			if (!usable.test(entry.getValue())) {
				continue;
			}
			String key = entry.getKey().toLowerCase(Locale.ROOT);
			if (key.equals(name)) {
				return Optional.of(entry);
			}
			if (key.endsWith("/" + name) && (prefixed == null || key.equals("openai/" + name))) {
				prefixed = entry;
			}
		}
		return Optional.ofNullable(prefixed);
	}

	private static boolean hasPrices(JsonNode entry) {
		return entry.path("input_cost_per_token").isNumber() && entry.path("output_cost_per_token").isNumber();
	}

	private static BigDecimal perMillion(JsonNode entry, String field) {
		BigDecimal perMillion = entry.path(field).decimalValue().multiply(MILLION).stripTrailingZeros();
		return perMillion.scale() < 0 ? perMillion.setScale(0) : perMillion;
	}

	private JsonNode catalog() {
		Cached current = cached;
		if (current != null && current.fetchedAt().plus(cacheFor).isAfter(clock.instant())) {
			return current.catalog();
		}
		synchronized (this) {
			if (cached != null && cached.fetchedAt().plus(cacheFor).isAfter(clock.instant())) {
				return cached.catalog();
			}
			try {
				HttpResponse<InputStream> response = httpClient.send(
						HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(30)).GET().build(),
						HttpResponse.BodyHandlers.ofInputStream());
				try (InputStream body = response.body()) {
					if (response.statusCode() != 200) {
						throw new IllegalStateException("HTTP " + response.statusCode());
					}
					JsonNode catalog = objectMapper.readTree(body);
					cached = new Cached(catalog, clock.instant());
					log.info("Token price catalog loaded from {}: {} entries", url, catalog.size());
					return catalog;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new PriceCatalogUnavailableException("Reading the token price catalog was interrupted.", e);
			} catch (Exception e) {
				log.warn("Token price catalog {} could not be read: {}", url, e.toString());
				if (cached != null) {
					// A stale catalog is better than none.
					return cached.catalog();
				}
				throw new PriceCatalogUnavailableException(
						"The token price catalog (" + url + ") could not be read: " + e.getMessage(), e);
			}
		}
	}
}
