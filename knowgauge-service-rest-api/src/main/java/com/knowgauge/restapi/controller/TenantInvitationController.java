package com.knowgauge.restapi.controller;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.TenantInvitationApi;
import com.knowgauge.contract.dto.TenantInvitationPreviewDto;
import com.knowgauge.contract.dto.TenantJoinedDto;
import com.knowgauge.core.service.tenant.TenantMembershipService;
import com.knowgauge.core.service.tenant.TenantMembershipService.Joined;
import com.knowgauge.restapi.mapper.TenantInvitationMapper;
import com.knowgauge.restapi.security.AccountResolver;
import com.knowgauge.restapi.security.SignedInAccount;

/**
 * The invitee's side of a workspace invitation (ADR015): the link {@code /join/{token}} shows the invitation before
 * signing in, and accepting needs the invited, verified email.
 */
@RestController
public class TenantInvitationController implements TenantInvitationApi {

	private final TenantMembershipService membershipService;
	private final TenantInvitationMapper tenantInvitationMapper;
	private final ObjectProvider<AccountResolver> accountResolver;

	public TenantInvitationController(TenantMembershipService membershipService, TenantInvitationMapper tenantInvitationMapper,
			ObjectProvider<AccountResolver> accountResolver) {
		this.membershipService = membershipService;
		this.tenantInvitationMapper = tenantInvitationMapper;
		this.accountResolver = accountResolver;
	}

	@Override
	public ResponseEntity<TenantInvitationPreviewDto> preview(String token) {
		return ResponseEntity.ok(tenantInvitationMapper.toDto(membershipService.preview(token)));
	}

	@Override
	public ResponseEntity<TenantJoinedDto> accept(String token) {
		Optional<SignedInAccount> account = SignedInAccount.current();
		Joined joined = membershipService.accept(token, account.map(SignedInAccount::emailVerified).orElse(false));
		// The next request must see the new workspace and roles, not the cached account.
		account.ifPresent(a -> accountResolver.ifAvailable(resolver -> resolver.evict(a.subject())));
		return ResponseEntity.ok(tenantInvitationMapper.toDto(joined));
	}
}
