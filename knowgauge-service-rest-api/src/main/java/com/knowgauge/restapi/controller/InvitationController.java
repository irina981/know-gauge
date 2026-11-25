package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.api.InvitationApi;
import com.knowgauge.contract.dto.InviteInput;
import com.knowgauge.contract.dto.TestInvitationDto;
import com.knowgauge.core.service.invitation.InvitationService;
import com.knowgauge.restapi.mapper.PublicMapper;
import com.knowgauge.restapi.security.Roles;

import jakarta.annotation.security.RolesAllowed;

/**
 * Personal invitations (ADR009): one link per email, sent by the server,
 * usable for one attempt.
 */
@RestController
public class InvitationController implements InvitationApi {

	private final InvitationService invitationService;
	private final PublicMapper publicMapper;

	public InvitationController(InvitationService invitationService, PublicMapper publicMapper) {
		this.invitationService = invitationService;
		this.publicMapper = publicMapper;
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<List<TestInvitationDto>> invite(Long testId, InviteInput input) {
		return ResponseEntity.ok(invitationService.invite(testId, input.emails(), input.message()).stream()
				.map(publicMapper::toInvitation).toList());
	}

	@Override
	public ResponseEntity<List<TestInvitationDto>> list(Long testId) {
		return ResponseEntity.ok(invitationService.list(testId).stream().map(publicMapper::toInvitation).toList());
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.TENANT_USER, Roles.ADMIN })
	public ResponseEntity<TestInvitationDto> resend(Long testId, Long invitationId) {
		return ResponseEntity.ok(publicMapper.toInvitation(invitationService.resend(testId, invitationId, null)));
	}
}
