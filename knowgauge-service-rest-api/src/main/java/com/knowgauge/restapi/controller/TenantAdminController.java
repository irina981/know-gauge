package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.TenantAdminApi;
import com.knowgauge.contract.dto.TenantCreateInput;
import com.knowgauge.contract.dto.TenantCreatedDto;
import com.knowgauge.contract.dto.TenantInvitationDto;
import com.knowgauge.contract.dto.TenantInviteInput;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.service.tenant.TenantProvisioningService;
import com.knowgauge.core.service.tenant.TenantProvisioningService.CreatedTenant;
import com.knowgauge.restapi.mapper.AdminMapper;
import com.knowgauge.restapi.mapper.TenantInvitationMapper;
import com.knowgauge.restapi.util.EnumParser;

/** An ADMIN creates workspaces and invites people to them (ADR015). Under /api/admin: ADMIN role only. */
@RestController
public class TenantAdminController implements TenantAdminApi {

	private final TenantProvisioningService tenantProvisioningService;
	private final AdminMapper adminMapper;
	private final TenantInvitationMapper tenantInvitationMapper;

	public TenantAdminController(TenantProvisioningService tenantProvisioningService, AdminMapper adminMapper,
			TenantInvitationMapper tenantInvitationMapper) {
		this.tenantProvisioningService = tenantProvisioningService;
		this.adminMapper = adminMapper;
		this.tenantInvitationMapper = tenantInvitationMapper;
	}

	@Override
	public ResponseEntity<TenantCreatedDto> create(TenantCreateInput input) {
		CreatedTenant created = tenantProvisioningService.create(EnumParser.parse(TenantType.class, input.type()),
				input.name(), input.emails(), input.message());
		return ResponseEntity.status(HttpStatus.CREATED).body(new TenantCreatedDto(adminMapper.toDto(created.tenant()),
				created.invitations().stream().map(tenantInvitationMapper::toDto).toList()));
	}

	@Override
	public ResponseEntity<List<TenantInvitationDto>> invitations(Long id) {
		return ResponseEntity.ok(tenantProvisioningService.listInvitations(id).stream().map(tenantInvitationMapper::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantInvitationDto>> invite(Long id, TenantInviteInput input) {
		return ResponseEntity.ok(tenantProvisioningService.invite(id, input.emails(), TenantController.roleOf(input), input.message()).stream()
				.map(tenantInvitationMapper::toDto).toList());
	}

	@Override
	public ResponseEntity<TenantInvitationDto> resend(Long id, Long invitationId) {
		return ResponseEntity.ok(tenantInvitationMapper.toDto(tenantProvisioningService.resend(id, invitationId, null)));
	}

	@Override
	public ResponseEntity<Void> revoke(Long id, Long invitationId) {
		tenantProvisioningService.revoke(id, invitationId);
		return ResponseEntity.noContent().build();
	}
}
