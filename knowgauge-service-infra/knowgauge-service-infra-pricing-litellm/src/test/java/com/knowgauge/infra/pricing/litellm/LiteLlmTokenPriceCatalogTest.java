package com.knowgauge.infra.pricing.litellm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class LiteLlmTokenPriceCatalogTest {

	private static JsonNode catalog() throws Exception {
		return new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree("""
				{
				  "sample_spec": { "input_cost_per_token": 0.0, "output_cost_per_token": 0.0 },
				  "gpt-4.1": { "input_cost_per_token": 2e-06, "output_cost_per_token": 8e-06, "litellm_provider": "openai" },
				  "azure/o9-mini": { "input_cost_per_token": 1.2e-06, "output_cost_per_token": 5e-06, "litellm_provider": "azure" },
				  "openai/o9-mini": { "input_cost_per_token": 1.1e-06, "output_cost_per_token": 4.4e-06, "litellm_provider": "openai" },
				  "text-embedding-3-small": { "input_cost_per_token": 2e-08, "litellm_provider": "openai" }
				}""");
	}

	@Test
	void anExactNameWinsIgnoringCase() throws Exception {
		assertThat(LiteLlmTokenPriceCatalog.find(catalog(), "GPT-4.1")).get().extracting(e -> e.getKey())
				.isEqualTo("gpt-4.1");
	}

	@Test
	void aProviderPrefixedNameIsFoundOpenAiFirst() throws Exception {
		assertThat(LiteLlmTokenPriceCatalog.find(catalog(), "o9-mini")).get().extracting(e -> e.getKey())
				.isEqualTo("openai/o9-mini");
	}

	@Test
	void chatModelsArePlainPricedNamesOfTheProvider() throws Exception {
		JsonNode catalog = new ObjectMapper().readTree("""
				{
				  "gpt-4.1": { "input_cost_per_token": 2e-06, "output_cost_per_token": 8e-06, "litellm_provider": "openai", "mode": "chat" },
				  "o9-pro": { "input_cost_per_token": 1e-05, "output_cost_per_token": 4e-05, "litellm_provider": "openai", "mode": "responses" },
				  "openai/gpt-4.1": { "input_cost_per_token": 2e-06, "output_cost_per_token": 8e-06, "litellm_provider": "openai", "mode": "chat" },
				  "ft:gpt-4.1": { "input_cost_per_token": 3e-06, "output_cost_per_token": 1.2e-05, "litellm_provider": "openai", "mode": "chat" },
				  "text-embedding-3-small": { "input_cost_per_token": 2e-08, "output_cost_per_token": 0.0, "litellm_provider": "openai", "mode": "embedding" },
				  "claude-x": { "input_cost_per_token": 3e-06, "output_cost_per_token": 1.5e-05, "litellm_provider": "anthropic", "mode": "chat" }
				}""");

		assertThat(LiteLlmTokenPriceCatalog.chatModels(catalog, "openai")).containsExactly("gpt-4.1", "o9-pro");
	}

	@Test
	void anEmbeddingModelNeedsOnlyAnInputPrice() throws Exception {
		JsonNode catalog = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree("""
				{ "text-embedding-3-small": { "input_cost_per_token": 2e-08, "output_cost_per_token": 0.0,
				    "output_vector_size": 1536, "litellm_provider": "openai", "mode": "embedding" },
				  "text-embedding-x": { "input_cost_per_token": 1e-07, "litellm_provider": "openai", "mode": "embedding" } }""");

		assertThat(LiteLlmTokenPriceCatalog.findEmbedding(catalog, "text-embedding-3-small")).get()
				.satisfies(e -> {
					assertThat(e.inputUsdPerMillion()).isEqualByComparingTo("0.02");
					assertThat(e.vectorSize()).isEqualTo(1536);
				});
		assertThat(LiteLlmTokenPriceCatalog.findEmbedding(catalog, "text-embedding-x")).get()
				.extracting(e -> e.vectorSize()).isNull();
	}

	@Test
	void entriesWithoutBothPricesAndUnknownModelsAreNotFound() throws Exception {
		assertThat(LiteLlmTokenPriceCatalog.find(catalog(), "text-embedding-3-small")).isEmpty();
		assertThat(LiteLlmTokenPriceCatalog.find(catalog(), "made-up-model")).isEmpty();
	}
}
