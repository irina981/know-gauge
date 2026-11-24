package com.knowgauge.restapi.controller;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.CurrentUserDto;
import com.knowgauge.contract.dto.UsageSummaryDto;
import com.knowgauge.contract.dto.UserPreferencesDto;
import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.service.account.AccountService;
import com.knowgauge.core.service.usage.UsageService;
import com.knowgauge.restapi.mapper.AccountMapper;
import com.knowgauge.restapi.security.SignedInAccount;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Account", description = "The signed-in user, their settings and usage")
public class AccountController {

	private final AccountService accountService;
	private final UsageService usageService;
	private final AccountMapper accountMapper;
	private final ExecutionContext executionContext;
	private final String securityMode;

	public AccountController(AccountService accountService, UsageService usageService, AccountMapper accountMapper,
			ExecutionContext executionContext, @Value("${kg.security.mode:fixed}") String securityMode) {
		this.accountService = accountService;
		this.usageService = usageService;
		this.accountMapper = accountMapper;
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
			Long userId = currentUserId();
			return ResponseEntity.ok(new CurrentUserDto(userId, tenantId, tenantName, null, null,
					List.of(Role.TENANT_ADMIN.name()), securityMode, null));
		}
		User user = account.get().user();
		// The user's roles from user_roles (ADR012), in the Role enum's order.
		List<String> roles = user.getRoles().stream().sorted().map(Role::name).toList();
		return ResponseEntity.ok(new CurrentUserDto(user.getId(), user.getTenantId(), tenantName, user.getEmail(),
				user.getDisplayName(), roles, securityMode, user.getCreatedAt()));
	}

	@GetMapping("/api/me/preferences")
	@Operation(summary = "Get the current user's settings", description = "Settings the user chose for the app. Fields never chosen are null. With sign-in off and no stored user, all fields are null.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)") })
	public ResponseEntity<UserPreferencesDto> preferences() {
		return ResponseEntity.ok(accountMapper.toDto(accountService.getPreferences(currentUserId())));
	}

	@PutMapping("/api/me/preferences")
	@Operation(summary = "Replace the current user's settings", description = "Stores all settings at once; a null field goes back to the app default.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Settings stored"),
			@ApiResponse(responseCode = "400", description = "Unknown value, for example an unknown theme"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)"),
			@ApiResponse(responseCode = "404", description = "No stored user (sign-in off)") })
	public ResponseEntity<UserPreferencesDto> updatePreferences(@RequestBody UserPreferencesDto input) {
		return ResponseEntity.ok(accountMapper
				.toDto(accountService.updatePreferences(currentUserId(), accountMapper.toDomain(input))));
	}

	@GetMapping("/api/me/usage")
	@Operation(summary = "Get the current workspace's usage", description = "What the user's own workspace holds (topics, documents, storage) and what it used this calendar month (UTC): tests, AI-written questions, submitted attempts and LLM tokens. The shared library is not counted.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Usage returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in (security mode oidc)") })
	public ResponseEntity<UsageSummaryDto> usage() {
		return ResponseEntity.ok(accountMapper.toDto(usageService.currentMonth()));
	}

	private Long currentUserId() {
		OptionalLong userId = executionContext.userId();
		return userId.isPresent() ? userId.getAsLong() : null;
	}
}
