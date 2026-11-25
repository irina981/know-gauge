package com.knowgauge.restapi.controller;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.AccountApi;
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

@RestController
public class AccountController implements AccountApi {

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

	@Override
	public ResponseEntity<CurrentUserDto> me() {
		Optional<SignedInAccount> account = SignedInAccount.current();
		long tenantId = executionContext.tenantId();
		Optional<Tenant> tenant = accountService.findTenant(tenantId);
		String tenantName = tenant.map(Tenant::getName).orElse(null);
		String tenantType = tenant.map(t -> t.getType().name()).orElse(null);
		if (account.isEmpty()) {
			// Security mode fixed: nobody signs in; the fixed user administers the fixed tenant.
			Long userId = currentUserId();
			return ResponseEntity.ok(new CurrentUserDto(userId, tenantId, tenantName, tenantType, null, null,
					List.of(Role.TENANT_ADMIN.name()), securityMode, null));
		}
		User user = account.get().user();
		// The user's roles from user_roles (ADR012), in the Role enum's order.
		List<String> roles = user.getRoles().stream().sorted().map(Role::name).toList();
		return ResponseEntity.ok(new CurrentUserDto(user.getId(), user.getTenantId(), tenantName, tenantType,
				user.getEmail(), user.getDisplayName(), roles, securityMode, user.getCreatedAt()));
	}

	@Override
	public ResponseEntity<UserPreferencesDto> preferences() {
		return ResponseEntity.ok(accountMapper.toDto(accountService.getPreferences(currentUserId())));
	}

	@Override
	public ResponseEntity<UserPreferencesDto> updatePreferences(UserPreferencesDto input) {
		return ResponseEntity.ok(accountMapper
				.toDto(accountService.updatePreferences(currentUserId(), accountMapper.toDomain(input))));
	}

	@Override
	public ResponseEntity<UsageSummaryDto> usage() {
		return ResponseEntity.ok(accountMapper.toDto(usageService.currentMonth()));
	}

	private Long currentUserId() {
		OptionalLong userId = executionContext.userId();
		return userId.isPresent() ? userId.getAsLong() : null;
	}
}
