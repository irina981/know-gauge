package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.TenantApi;
import com.knowgauge.contract.dto.TenantInvitationDto;
import com.knowgauge.contract.dto.TenantInviteInput;
import com.knowgauge.contract.dto.TenantSettingDto;
import com.knowgauge.contract.dto.TenantSettingsInput;
import com.knowgauge.contract.dto.TenantUserDto;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.service.tenant.OrganizationTenantService;
import com.knowgauge.restapi.mapper.TenantInvitationMapper;
import com.knowgauge.restapi.security.Roles;
import com.knowgauge.restapi.util.EnumParser;

import jakarta.annotation.security.RolesAllowed;

/**
 * The signed-in user's own organization workspace, for its tenant admins (ADR016). Everything under /api/tenant
 * needs the TENANT_ADMIN role (SecurityConfig); the service refuses workspaces that are not organizations.
 */
@RolesAllowed(Roles.TENANT_ADMIN)
@RestController
public class TenantController implements TenantApi {

	private final OrganizationTenantService tenantService;
	private final TenantInvitationMapper tenantInvitationMapper;

	public TenantController(OrganizationTenantService tenantService, TenantInvitationMapper tenantInvitationMapper) {
		this.tenantService = tenantService;
		this.tenantInvitationMapper = tenantInvitationMapper;
	}

	@Override
	public ResponseEntity<List<TenantUserDto>> tenantUsers() {
		return ResponseEntity.ok(tenantService.users().stream().map(TenantController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantInvitationDto>> tenantInvitations() {
		return ResponseEntity.ok(tenantService.invitations().stream().map(tenantInvitationMapper::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantInvitationDto>> inviteToTenant(TenantInviteInput input) {
		return ResponseEntity.ok(tenantService.invite(input.emails(), roleOf(input), input.message()).stream()
				.map(tenantInvitationMapper::toDto).toList());
	}

	@Override
	public ResponseEntity<TenantInvitationDto> resendTenantInvitation(Long invitationId) {
		return ResponseEntity.ok(tenantInvitationMapper.toDto(tenantService.resendInvitation(invitationId)));
	}

	@Override
	public ResponseEntity<Void> revokeTenantInvitation(Long invitationId) {
		tenantService.revokeInvitation(invitationId);
		return ResponseEntity.noContent().build();
	}

	/** The invitation's role; TENANT_ADMIN when it names none. Also used by {@link TenantAdminController}. */
	static Role roleOf(TenantInviteInput input) {
		Role role = EnumParser.parse(Role.class, input.role());
		return role == null ? Role.TENANT_ADMIN : role;
	}

	@Override
	public ResponseEntity<List<TenantSettingDto>> tenantSettings() {
		return ResponseEntity.ok(tenantService.settings().stream().map(AdminController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantSettingDto>> saveTenantSettings(TenantSettingsInput input) {
		return ResponseEntity.ok(
				tenantService.saveSettings(input.values()).stream().map(AdminController::toDto).toList());
	}

	@Override
	public ResponseEntity<List<TenantSettingDto>> resetTenantSettings() {
		return ResponseEntity.ok(tenantService.resetSettings().stream().map(AdminController::toDto).toList());
	}

	private static TenantUserDto toDto(User user) {
		List<String> roles = user.getRoles().stream().sorted().map(Role::name).toList();
		return new TenantUserDto(user.getId(), user.getEmail(), user.getDisplayName(), roles, user.getCreatedAt(),
				user.getLastSeenAt());
	}
}
