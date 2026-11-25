package com.knowgauge.contract.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.knowgauge.contract.dto.EmbeddingInfoDto;
import com.knowgauge.contract.dto.SystemSettingDto;
import com.knowgauge.contract.dto.SystemSettingInput;
import com.knowgauge.contract.dto.TokenPriceDto;
import com.knowgauge.contract.dto.TokenPriceInput;
import com.knowgauge.contract.dto.TokenPriceSuggestionDto;
import com.knowgauge.contract.dto.TokenPricesDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** System-wide settings, for the System settings page (ADMIN role, like all of /api/admin/**). */
@Tag(name = "System settings", description = "System-wide settings (ADMIN role): settings with defaults from the properties files, and LLM token prices")
public interface SystemSettingsApi {

	@GetMapping("/api/admin/settings/embedding")
	@Operation(summary = "Get the embedding model", description = "Read-only: the embedding model, its vector size, its price in the price catalog and the stored embeddings per model. Changing the model means re-ingesting every document, so it is set in the properties files (kg.embedding.openai.model).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Embedding model returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<EmbeddingInfoDto> embedding();

	@GetMapping("/api/admin/settings/values")
	@Operation(summary = "List the system settings", description = "Every setting an admin may change, with its default from the properties files, the override saved here (if any) and the value in effect.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<List<SystemSettingDto>> settings();

	@PutMapping("/api/admin/settings/values")
	@Operation(summary = "Save system settings", description = "Saves the given values as overrides and applies them at once; they also apply after a restart. All values are checked first: if one does not fit, nothing is saved. An empty value means empty for an optional setting (e.g. no limit).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings saved; every setting returned"),
			@ApiResponse(responseCode = "400", description = "A value does not fit its setting (all problems in the message)"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "Unknown setting") })
	ResponseEntity<List<SystemSettingDto>> saveSettings(@Valid @RequestBody SystemSettingInput input);

	@DeleteMapping("/api/admin/settings/values")
	@Operation(summary = "Reset all system settings to their defaults", description = "Removes every saved override from the database, so the values from the properties files apply again. Token prices are not touched.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings reset; every setting returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<List<SystemSettingDto>> resetSettings();

	@GetMapping("/api/admin/settings/token-prices")
	@Operation(summary = "List the token prices", description = "Every model's price in US dollars per million tokens, used to show what the LLM tokens on the usage page cost, and the model names to offer when adding one.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Prices returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<TokenPricesDto> tokenPrices();

	@PutMapping("/api/admin/settings/token-prices/{model}")
	@Operation(summary = "Set a model's token price", description = "Adds the price, or changes it. The model name matches recorded LLM calls exactly or as a prefix (e.g. gpt-4.1 for gpt-4.1-2025-04-14); the longest match wins.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Price saved"),
			@ApiResponse(responseCode = "400", description = "Missing or negative price, or an invalid model name"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<TokenPriceDto> setTokenPrice(@PathVariable String model, @Valid @RequestBody TokenPriceInput input);

	@DeleteMapping("/api/admin/settings/token-prices/{model}")
	@Operation(summary = "Remove a model's token price", description = "Its tokens are then shown as not priced.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Price removed"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "The model has no price") })
	ResponseEntity<Void> deleteTokenPrice(@PathVariable String model);

	@PostMapping("/api/admin/settings/token-prices/suggestion")
	@Operation(summary = "Look up a model's prices in the price catalog", description = "Looks the model up in LiteLLM's public price catalog (exact name, else a provider-prefixed name such as openai/<model>). Nothing is saved. Third-party data: check it against the provider's price list.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Suggestion returned (known=false when the catalog does not list the model)"),
			@ApiResponse(responseCode = "400", description = "No model name"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "502", description = "The price catalog could not be downloaded or read") })
	ResponseEntity<TokenPriceSuggestionDto> suggestTokenPrice(@RequestParam String model);
}
