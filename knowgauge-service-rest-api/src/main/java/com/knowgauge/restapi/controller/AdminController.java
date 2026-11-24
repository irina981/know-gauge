package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.TenantSummaryDto;
import com.knowgauge.contract.dto.WorkspaceSettingDto;
import com.knowgauge.contract.dto.WorkspaceSettingsInput;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.core.service.settings.SettingDefinition;
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.restapi.mapper.AdminMapper;
import com.knowgauge.restapi.util.EnumParser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Application administration. Everything under /api/admin needs the ADMIN role (SecurityConfig, ADR012). */
@RestController
@Tag(name = "Admin", description = "Application administration (ADMIN role)")
public class AdminController {

	private final TenantAdminService tenantAdminService;
	private final TenantSettingsService tenantSettingsService;
	private final AdminMapper adminMapper;

	public AdminController(TenantAdminService tenantAdminService, TenantSettingsService tenantSettingsService,
			AdminMapper adminMapper) {
		this.tenantAdminService = tenantAdminService;
		this.tenantSettingsService = tenantSettingsService;
		this.adminMapper = adminMapper;
	}

	@GetMapping("/api/admin/tenants/{id}")
	@Operation(summary = "Get one tenant", description = "A tenant with its type, owner, content counts and limits in effect (ADR013, ADR014).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Tenant returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant") })
	public ResponseEntity<TenantSummaryDto> tenant(@PathVariable Long id) {
		return ResponseEntity.ok(adminMapper.toDto(tenantAdminService.getTenant(id)));
	}

	@GetMapping("/api/admin/tenants")
	@Operation(summary = "List tenants (filtered, sorted, paged)", description = "Tenants (user workspaces and libraries) with their type, owner and content counts. search matches the name and the owner's name or email; type keeps one tenant type. Sort by name (default), type, owner, topics, documents, storage (bytes of documents), usage (this month's tokens) or createdAt. Each tenant comes with its storage and its LLM tokens this calendar month (UTC), each with its limit. An ADMIN reads any of them through the normal content API: send the tenant id in the X-KG-Tenant header on GET requests to the workspace APIs, which then return that tenant's data, read-only (ADR013).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Tenants returned"),
			@ApiResponse(responseCode = "400", description = "Unknown type or sort property"),
			@ApiResponse(responseCode = "401", description = "Not signed in"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<Page<TenantSummaryDto>> tenants(@RequestParam(required = false) String search,
			@RequestParam(required = false) String type, @PageableDefault(size = 20, sort = "name") Pageable pageable) {
		TenantSearch filter = new TenantSearch(search, EnumParser.parse(TenantType.class, type));
		return ResponseEntity.ok(tenantAdminService.listTenants(filter, pageable).map(adminMapper::toDto));
	}

	@GetMapping("/api/admin/tenants/{id}/settings")
	@Operation(summary = "List a workspace's settings", description = "The workspace-level settings (for now the workspace limits) of one workspace: the system value, the workspace's own value if it has one, and the value in effect (ADR014).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant") })
	public ResponseEntity<List<WorkspaceSettingDto>> tenantSettings(@PathVariable Long id) {
		return ResponseEntity.ok(tenantSettingsService.list(id).stream().map(AdminController::toDto).toList());
	}

	@PutMapping("/api/admin/tenants/{id}/settings")
	@Operation(summary = "Save a workspace's settings", description = "Saves the workspace's own values, which override the system values for it and apply at once. An empty value removes the workspace's own value. For a limit, 0 means no limit. All values are checked first: if one does not fit, nothing is saved.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings saved; the workspace's settings returned"),
			@ApiResponse(responseCode = "400", description = "A value does not fit, or the setting applies to the whole system only"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant or setting") })
	public ResponseEntity<List<WorkspaceSettingDto>> saveTenantSettings(@PathVariable Long id,
			@Valid @RequestBody WorkspaceSettingsInput input) {
		return ResponseEntity
				.ok(tenantSettingsService.setAll(id, input.values()).stream().map(AdminController::toDto).toList());
	}

	@DeleteMapping("/api/admin/tenants/{id}/settings")
	@Operation(summary = "Reset a workspace's settings", description = "Removes all of the workspace's own values, so the system values apply to it again.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings reset; the workspace's settings returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant") })
	public ResponseEntity<List<WorkspaceSettingDto>> resetTenantSettings(@PathVariable Long id) {
		return ResponseEntity.ok(tenantSettingsService.resetAll(id).stream().map(AdminController::toDto).toList());
	}

	private static WorkspaceSettingDto toDto(TenantSettingsService.TenantSettingValue setting) {
		SettingDefinition d = setting.definition();
		return new WorkspaceSettingDto(d.key(), d.label(), d.description(), d.type().name(), d.min(), d.max(),
				setting.systemValue(), setting.ownValue(), setting.value());
	}
}
