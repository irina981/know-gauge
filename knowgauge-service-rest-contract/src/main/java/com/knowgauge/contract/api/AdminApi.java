package com.knowgauge.contract.api;

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

import com.knowgauge.contract.dto.TenantSettingDto;
import com.knowgauge.contract.dto.TenantSettingsInput;
import com.knowgauge.contract.dto.TenantSummaryDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Application administration: tenants and their settings. Everything under /api/admin needs the ADMIN role. */
@Tag(name = "Admin", description = "Application administration (ADMIN role)")
public interface AdminApi {

	@GetMapping("/api/admin/tenants/{id}")
	@Operation(summary = "Get one tenant", description = "A tenant with its type, owner, content counts and limits in effect (ADR013, ADR014).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Tenant returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant") })
	ResponseEntity<TenantSummaryDto> tenant(@PathVariable Long id);

	@GetMapping("/api/admin/tenants")
	@Operation(summary = "List tenants (filtered, sorted, paged)", description = "Tenants (user workspaces and libraries) with their type, owner and content counts. search matches the name and the owner's name or email; type keeps one tenant type. Sort by name (default), type, owner, topics, documents, storage (bytes of documents), usage (this month's tokens) or createdAt. Each tenant comes with its storage and its LLM tokens this calendar month (UTC), each with its limit. An ADMIN reads any of them through the normal content API: send the tenant id in the X-KG-Tenant header on GET requests to the tenant APIs, which then return that tenant's data, read-only (ADR013).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Tenants returned"),
			@ApiResponse(responseCode = "400", description = "Unknown type or sort property"),
			@ApiResponse(responseCode = "401", description = "Not signed in"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<Page<TenantSummaryDto>> tenants(@RequestParam(required = false) String search,
			@RequestParam(required = false) String type, @PageableDefault(size = 20, sort = "name") Pageable pageable);

	@GetMapping("/api/admin/tenants/{id}/settings")
	@Operation(summary = "List a tenant's settings", description = "The tenant-level settings (for now the tenant limits) of one tenant: the system value, the tenant's own value if it has one, and the value in effect (ADR014).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant") })
	ResponseEntity<List<TenantSettingDto>> tenantSettings(@PathVariable Long id);

	@PutMapping("/api/admin/tenants/{id}/settings")
	@Operation(summary = "Save a tenant's settings", description = "Saves the tenant's own values, which override the system values for it and apply at once. An empty value removes the tenant's own value. For a limit, 0 means no limit. All values are checked first: if one does not fit, nothing is saved.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings saved; the tenant's settings returned"),
			@ApiResponse(responseCode = "400", description = "A value does not fit, or the setting applies to the whole system only"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant or setting") })
	ResponseEntity<List<TenantSettingDto>> saveTenantSettings(@PathVariable Long id,
			@Valid @RequestBody TenantSettingsInput input);

	@DeleteMapping("/api/admin/tenants/{id}/settings")
	@Operation(summary = "Reset a tenant's settings", description = "Removes all of the tenant's own values, so the system values apply to it again.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings reset; the tenant's settings returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such tenant") })
	ResponseEntity<List<TenantSettingDto>> resetTenantSettings(@PathVariable Long id);
}
