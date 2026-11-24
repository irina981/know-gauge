package com.knowgauge.restapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.TenantSummaryDto;
import com.knowgauge.core.model.TenantSearch;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.restapi.mapper.AdminMapper;
import com.knowgauge.restapi.util.EnumParser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Application administration. Everything under /api/admin needs the ADMIN role (SecurityConfig, ADR012). */
@RestController
@Tag(name = "Admin", description = "Application administration (ADMIN role)")
public class AdminController {

	private final TenantAdminService tenantAdminService;
	private final AdminMapper adminMapper;

	public AdminController(TenantAdminService tenantAdminService, AdminMapper adminMapper) {
		this.tenantAdminService = tenantAdminService;
		this.adminMapper = adminMapper;
	}

	@GetMapping("/api/admin/tenants/{id}")
	@Operation(summary = "Get one tenant", description = "A tenant with its type, owner and content counts (ADR013).")
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
}
