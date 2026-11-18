package com.knowgauge.restapi.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.CurrentUserDto;
import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.service.account.AccountService;
import com.knowgauge.restapi.security.SignedInAccount;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Account", description = "The signed-in user")
public class AccountController {

	private final AccountService accountService;
	private final ExecutionContext executionContext;
	private final String securityMode;

	public AccountController(AccountService accountService, ExecutionContext executionContext,
			@Value("${kg.security.mode:fixed}") String securityMode) {
		this.accountService = accountService;
		this.executionContext = executionContext;
		this.securityMode = securityMode;
	}

	@GetMapping("/api/me")
	@Operation(summary = "Get the current user", description = "The signed-in user, their tenant and roles. The first call with a new user's token creates the user and a personal tenant. With sign-in off, the fixed tenant and user.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Current user returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)") })
	public ResponseEntity<CurrentUserDto> me() {
		Optional<SignedInAccount> account = SignedInAccount.current();
		long tenantId = executionContext.tenantId();
		String tenantName = accountService.findTenant(tenantId).map(Tenant::getName).orElse(null);
		if (account.isEmpty()) {
			// Security mode fixed: nobody signs in; the fixed user administers the fixed tenant.
			Long userId = executionContext.userId().isPresent() ? executionContext.userId().getAsLong() : null;
			return ResponseEntity.ok(new CurrentUserDto(userId, tenantId, tenantName, null, null,
					List.of(Role.TENANT_ADMIN.name()), securityMode));
		}
		User user = account.get().user();
		List<String> roles = new ArrayList<>();
		if (account.get().applicationAdmin()) {
			roles.add(Role.APPLICATION_ADMIN.name());
		}
		if (user.getRole() != null) {
			roles.add(user.getRole().name());
		}
		return ResponseEntity.ok(new CurrentUserDto(user.getId(), user.getTenantId(), tenantName, user.getEmail(),
				user.getDisplayName(), roles, securityMode));
	}
}
