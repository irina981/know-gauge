package com.knowgauge.restapi.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowgauge.contract.dto.InviteInput;
import com.knowgauge.contract.dto.TestInvitationDto;
import com.knowgauge.core.service.invitation.InvitationService;
import com.knowgauge.restapi.mapper.PublicMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Personal invitations (ADR009): one link per email, sent by the server,
 * usable for one attempt.
 */
@RestController
@RequestMapping("/api/tests/{testId}/invitations")
@Tag(name = "Invitations", description = "Send personal test links by email")
public class InvitationController {

	private final InvitationService invitationService;
	private final PublicMapper publicMapper;

	public InvitationController(InvitationService invitationService, PublicMapper publicMapper) {
		this.invitationService = invitationService;
		this.publicMapper = publicMapper;
	}

	@PostMapping
	@Operation(summary = "Invite by email", description = "Sends each address its own link. Already invited addresses get their link again unless it was used. A failed send does not fail the request: it is reported in sendError and can be resent.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitations of the given addresses"),
			@ApiResponse(responseCode = "400", description = "No, too many or invalid addresses"),
			@ApiResponse(responseCode = "404", description = "Test not found"),
			@ApiResponse(responseCode = "409", description = "Test is not published") })
	public ResponseEntity<List<TestInvitationDto>> invite(@PathVariable Long testId,
			@RequestBody @Valid InviteInput input) {
		return ResponseEntity.ok(invitationService.invite(testId, input.emails(), input.message()).stream()
				.map(publicMapper::toInvitation).toList());
	}

	@GetMapping
	@Operation(summary = "List invitations", description = "Invitations of the test, oldest first, with the attempt and score of used links.")
	public ResponseEntity<List<TestInvitationDto>> list(@PathVariable Long testId) {
		return ResponseEntity.ok(invitationService.list(testId).stream().map(publicMapper::toInvitation).toList());
	}

	@PostMapping("/{invitationId}/resend")
	@Operation(summary = "Resend an invitation", description = "Sends the same personal link again.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Sent, or the send error is reported in sendError"),
			@ApiResponse(responseCode = "404", description = "Invitation not found"),
			@ApiResponse(responseCode = "409", description = "Link already used or the test is not published") })
	public ResponseEntity<TestInvitationDto> resend(@PathVariable Long testId, @PathVariable Long invitationId) {
		return ResponseEntity.ok(publicMapper.toInvitation(invitationService.resend(testId, invitationId, null)));
	}
}
