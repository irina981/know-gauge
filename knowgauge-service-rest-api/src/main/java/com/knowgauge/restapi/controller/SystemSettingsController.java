package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.EmbeddingInfoDto;
import com.knowgauge.contract.dto.SystemSettingDto;
import com.knowgauge.contract.dto.SystemSettingInput;
import com.knowgauge.contract.dto.TokenPriceDto;
import com.knowgauge.contract.dto.TokenPriceInput;
import com.knowgauge.contract.dto.TokenPriceSuggestionDto;
import com.knowgauge.contract.dto.TokenPricesDto;
import com.knowgauge.core.service.settings.EmbeddingSettingsService;
import com.knowgauge.core.service.settings.SettingDefinition;
import com.knowgauge.core.service.settings.SystemSettingsService;
import com.knowgauge.core.service.settings.TokenPriceService;
import com.knowgauge.restapi.mapper.AdminMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** System-wide settings, for the System settings page (ADMIN role, like all of /api/admin/**). */
@RestController
@Tag(name = "System settings", description = "System-wide settings (ADMIN role): settings with defaults from the properties files, and LLM token prices")
public class SystemSettingsController {

	private final TokenPriceService tokenPriceService;
	private final SystemSettingsService systemSettingsService;
	private final EmbeddingSettingsService embeddingSettingsService;
	private final AdminMapper adminMapper;

	public SystemSettingsController(TokenPriceService tokenPriceService, SystemSettingsService systemSettingsService,
			EmbeddingSettingsService embeddingSettingsService, AdminMapper adminMapper) {
		this.embeddingSettingsService = embeddingSettingsService;
		this.tokenPriceService = tokenPriceService;
		this.systemSettingsService = systemSettingsService;
		this.adminMapper = adminMapper;
	}

	@GetMapping("/api/admin/settings/embedding")
	@Operation(summary = "Get the embedding model", description = "Read-only: the embedding model, its vector size, its price in the price catalog and the stored embeddings per model. Changing the model means re-ingesting every document, so it is set in the properties files (kg.embedding.openai.model).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Embedding model returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<EmbeddingInfoDto> embedding() {
		EmbeddingSettingsService.EmbeddingInfo info = embeddingSettingsService.info();
		return ResponseEntity.ok(new EmbeddingInfoDto(info.model(), info.dimension(), info.catalogModel(),
				info.inputUsdPerMillion(), info.catalogVectorSize(),
				info.byModel().stream().map(c -> new EmbeddingInfoDto.EmbeddingModelCountDto(c.model(),
						c.embeddings(), c.documents())).toList()));
	}

	@GetMapping("/api/admin/settings/values")
	@Operation(summary = "List the system settings", description = "Every setting an admin may change, with its default from the properties files, the override saved here (if any) and the value in effect.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<List<SystemSettingDto>> settings() {
		return ResponseEntity.ok(systemSettingsService.list().stream().map(SystemSettingsController::toDto).toList());
	}

	@PutMapping("/api/admin/settings/values")
	@Operation(summary = "Save system settings", description = "Saves the given values as overrides and applies them at once; they also apply after a restart. All values are checked first: if one does not fit, nothing is saved. An empty value means empty for an optional setting (e.g. no limit).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings saved; every setting returned"),
			@ApiResponse(responseCode = "400", description = "A value does not fit its setting (all problems in the message)"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "Unknown setting") })
	public ResponseEntity<List<SystemSettingDto>> saveSettings(@Valid @RequestBody SystemSettingInput input) {
		return ResponseEntity.ok(systemSettingsService.setAll(input.values()).stream()
				.map(SystemSettingsController::toDto).toList());
	}

	@DeleteMapping("/api/admin/settings/values")
	@Operation(summary = "Reset all system settings to their defaults", description = "Removes every saved override from the database, so the values from the properties files apply again. Token prices are not touched.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings reset; every setting returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<List<SystemSettingDto>> resetSettings() {
		return ResponseEntity.ok(systemSettingsService.resetAll().stream().map(SystemSettingsController::toDto).toList());
	}

	private static SystemSettingDto toDto(SystemSettingsService.SystemSettingValue setting) {
		SettingDefinition d = setting.definition();
		return new SystemSettingDto(d.key(), d.group(), d.label(), d.description(), d.type().name(), d.options(),
				d.min(), d.max(), d.optional(), setting.defaultValue(), setting.overridden(), setting.overrideValue(),
				setting.value(), setting.suggestions(), d.tenantLevel(), d.settingType().name());
	}

	@GetMapping("/api/admin/settings/token-prices")
	@Operation(summary = "List the token prices", description = "Every model's price in US dollars per million tokens, used to show what the LLM tokens on the usage page cost, and the model names to offer when adding one.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Prices returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<TokenPricesDto> tokenPrices() {
		return ResponseEntity.ok(new TokenPricesDto(
				tokenPriceService.list().stream().map(adminMapper::toDto).toList(), tokenPriceService.knownModels(),
				tokenPriceService.catalogModels()));
	}

	@PutMapping("/api/admin/settings/token-prices/{model}")
	@Operation(summary = "Set a model's token price", description = "Adds the price, or changes it. The model name matches recorded LLM calls exactly or as a prefix (e.g. gpt-4.1 for gpt-4.1-2025-04-14); the longest match wins.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Price saved"),
			@ApiResponse(responseCode = "400", description = "Missing or negative price, or an invalid model name"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<TokenPriceDto> setTokenPrice(@PathVariable String model,
			@Valid @RequestBody TokenPriceInput input) {
		return ResponseEntity.ok(adminMapper.toDto(
				tokenPriceService.save(model, input.inputUsdPerMillion(), input.outputUsdPerMillion())));
	}

	@DeleteMapping("/api/admin/settings/token-prices/{model}")
	@Operation(summary = "Remove a model's token price", description = "Its tokens are then shown as not priced.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Price removed"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "The model has no price") })
	public ResponseEntity<Void> deleteTokenPrice(@PathVariable String model) {
		tokenPriceService.delete(model);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/admin/settings/token-prices/suggestion")
	@Operation(summary = "Look up a model's prices in the price catalog", description = "Looks the model up in LiteLLM's public price catalog (exact name, else a provider-prefixed name such as openai/<model>). Nothing is saved. Third-party data: check it against the provider's price list.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Suggestion returned (known=false when the catalog does not list the model)"),
			@ApiResponse(responseCode = "400", description = "No model name"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "502", description = "The price catalog could not be downloaded or read") })
	public ResponseEntity<TokenPriceSuggestionDto> suggestTokenPrice(@RequestParam String model) {
		return ResponseEntity.ok(adminMapper.toDto(tokenPriceService.suggest(model)));
	}
}
