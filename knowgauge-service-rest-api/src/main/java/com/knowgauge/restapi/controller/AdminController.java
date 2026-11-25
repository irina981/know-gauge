package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.AdminApi;
import com.knowgauge.contract.dto.TenantSettingDto;
import com.knowgauge.contract.dto.TenantSettingsInput;
import com.knowgauge.contract.dto.TenantSummaryDto;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.core.service.settings.SettingDefinition;
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.restapi.mapper.AdminMapper;
import com.knowgauge.restapi.util.EnumParser;

/** Application administration. Everything under /api/admin needs the ADMIN role (SecurityConfig, ADR012). */
@RestController
public class AdminController implements AdminApi {

	private final TenantAdminService tenantAdminService;
	private final TenantSettingsService tenantSettingsService;
	private final AdminMapper adminMapper;

	public AdminController(TenantAdminService tenantAdminService, TenantSettingsService tenantSettingsService,
			AdminMapper adminMapper) {
		this.tenantAdminService = tenantAdminService;
		this.tenantSettingsService = tenantSettingsService;
		this.adminMapper = adminMapper;
	}

	@Override
	public ResponseEntity<TenantSummaryDto> tenant(Long id) {
		return ResponseEntity.ok(adminMapper.toDto(tenantAdminService.getTenant(id)));
	}

	@Override
	public ResponseEntity<Page<TenantSummaryDto>> tenants(String search, String type, Pageable pageable) {
		TenantSearch filter = new TenantSearch(search, EnumParser.parse(TenantType.class, type));
		return ResponseEntity.ok(tenantAdminService.listTenants(filter, pageable).map(adminMapper::toDto));
	}

	@Override
	public ResponseEntity<List<TenantSettingDto>> tenantSettings(Long id) {
		return ResponseEntity.ok(tenantSettingsService.list(id).stream().map(AdminController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantSettingDto>> saveTenantSettings(Long id, TenantSettingsInput input) {
		return ResponseEntity
				.ok(tenantSettingsService.setAll(id, input.values()).stream().map(AdminController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantSettingDto>> resetTenantSettings(Long id) {
		return ResponseEntity.ok(tenantSettingsService.resetAll(id).stream().map(AdminController::toDto).toList());
	}

	/** Also used by {@link TenantController}. */
	static TenantSettingDto toDto(TenantSettingsService.TenantSettingValue setting) {
		SettingDefinition d = setting.definition();
		return new TenantSettingDto(d.key(), d.group(), d.label(), d.description(), d.type().name(), d.min(), d.max(),
				d.options(), setting.suggestions(), setting.config().getType().name(), setting.systemValue(),
				setting.ownValue(), setting.value());
	}
}
