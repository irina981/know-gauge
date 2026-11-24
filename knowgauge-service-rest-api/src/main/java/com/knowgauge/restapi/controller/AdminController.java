package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.TenantSummaryDto;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.restapi.mapper.AdminMapper;

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

	@GetMapping("/api/admin/tenants")
	@Operation(summary = "List all tenants", description = "Every tenant (user workspaces and libraries) with its type, owner and content counts, by name. An ADMIN reads any of them through the normal content API: send the tenant id in the X-KG-Tenant header on GET /api/content/** requests, which then return that tenant's content, read-only (ADR013).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Tenants returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	public ResponseEntity<List<TenantSummaryDto>> tenants() {
		return ResponseEntity.ok(adminMapper.toDtos(tenantAdminService.listTenants()));
	}
}
