package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.SystemSettingsApi;
import com.knowgauge.contract.dto.EmbeddingInfoDto;
import com.knowgauge.contract.dto.SystemSettingDto;
import com.knowgauge.contract.dto.SystemSettingInput;
import com.knowgauge.contract.dto.TokenPriceDto;
import com.knowgauge.contract.dto.TokenPriceInput;
import com.knowgauge.contract.dto.TokenPriceSuggestionDto;
import com.knowgauge.contract.dto.TokenPricesDto;
import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.service.settings.EmbeddingSettingsService;
import com.knowgauge.core.service.settings.SettingDefinition;
import com.knowgauge.core.service.settings.SystemSettingsService;
import com.knowgauge.core.service.settings.TokenPriceService;
import com.knowgauge.restapi.mapper.AdminMapper;

/** System-wide settings, for the System settings page (ADMIN role, like all of /api/admin/**). */
@RestController
public class SystemSettingsController implements SystemSettingsApi {

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

	@Override
	public ResponseEntity<EmbeddingInfoDto> embedding() {
		EmbeddingSettingsService.EmbeddingInfo info = embeddingSettingsService.info();
		return ResponseEntity.ok(new EmbeddingInfoDto(info.model(), info.dimension(), info.catalogModel(),
				info.inputUsdPerMillion(), info.catalogVectorSize(),
				info.byModel().stream().map(c -> new EmbeddingInfoDto.EmbeddingModelCountDto(c.model(),
						c.embeddings(), c.documents())).toList()));
	}

	@Override
	public ResponseEntity<List<SystemSettingDto>> settings() {
		return ResponseEntity.ok(systemSettingsService.list().stream().map(SystemSettingsController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<SystemSettingDto>> saveSettings(SystemSettingInput input) {
		return ResponseEntity.ok(systemSettingsService.setAll(input.values()).stream()
				.map(SystemSettingsController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<SystemSettingDto>> resetSettings() {
		return ResponseEntity.ok(systemSettingsService.resetAll().stream().map(SystemSettingsController::toDto).toList());
	}

	private static SystemSettingDto toDto(SystemSettingsService.SystemSettingValue setting) {
		SettingDefinition d = setting.definition();
		return new SystemSettingDto(d.key(), d.group(), d.label(), d.description(), d.type().name(), d.options(),
				d.min(), d.max(), d.optional(), setting.defaultValue(), setting.overridden(), setting.overrideValue(),
				setting.value(), setting.suggestions(), setting.config().allows(SettingOwner.TENANT),
				setting.config().getType().name());
	}

	@Override
	public ResponseEntity<TokenPricesDto> tokenPrices() {
		return ResponseEntity.ok(new TokenPricesDto(
				tokenPriceService.list().stream().map(adminMapper::toDto).toList(), tokenPriceService.knownModels(),
				tokenPriceService.catalogModels()));
	}

	@Override
	public ResponseEntity<TokenPriceDto> setTokenPrice(String model, TokenPriceInput input) {
		return ResponseEntity.ok(adminMapper.toDto(
				tokenPriceService.save(model, input.inputUsdPerMillion(), input.outputUsdPerMillion())));
	}

	@Override
	public ResponseEntity<Void> deleteTokenPrice(String model) {
		tokenPriceService.delete(model);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<TokenPriceSuggestionDto> suggestTokenPrice(String model) {
		return ResponseEntity.ok(adminMapper.toDto(tokenPriceService.suggest(model)));
	}
}
