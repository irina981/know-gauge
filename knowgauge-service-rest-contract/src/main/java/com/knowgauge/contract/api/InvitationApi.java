package com.knowgauge.contract.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.knowgauge.contract.dto.InviteInput;
import com.knowgauge.contract.dto.TestInvitationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Personal invitations (ADR009): one link per email, sent by the server,
 * usable for one attempt.
 */
@RequestMapping("/api/tests/{testId}/invitations")
@Tag(name = "Invitations", description = "Send personal test links by email")
public interface InvitationApi {

	@PostMapping
	@Operation(summary = "Invite by email", description = "Sends each address its own link. Already invited addresses get their link again unless it was used. A failed send does not fail the request: it is reported in sendError and can be resent.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitations of the given addresses"),
			@ApiResponse(responseCode = "400", description = "No, too many or invalid addresses"),
			@ApiResponse(responseCode = "404", description = "Test not found"),
			@ApiResponse(responseCode = "409", description = "Test is not published") })
	ResponseEntity<List<TestInvitationDto>> invite(@PathVariable Long testId, @RequestBody @Valid InviteInput input);

	@GetMapping
	@Operation(summary = "List invitations", description = "Invitations of the test, oldest first, with the attempt and score of used links.")
	ResponseEntity<List<TestInvitationDto>> list(@PathVariable Long testId);

	@PostMapping("/{invitationId}/resend")
	@Operation(summary = "Resend an invitation", description = "Sends the same personal link again.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Sent, or the send error is reported in sendError"),
			@ApiResponse(responseCode = "404", description = "Invitation not found"),
			@ApiResponse(responseCode = "409", description = "Link already used or the test is not published") })
	ResponseEntity<TestInvitationDto> resend(@PathVariable Long testId, @PathVariable Long invitationId);
}
