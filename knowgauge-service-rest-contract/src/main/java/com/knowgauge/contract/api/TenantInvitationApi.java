package com.knowgauge.contract.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.knowgauge.contract.dto.TenantInvitationPreviewDto;
import com.knowgauge.contract.dto.TenantJoinedDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The invitee's side of a workspace invitation (ADR015): the link {@code /join/{token}} shows the invitation before
 * signing in, and accepting needs the invited, verified email.
 */
@Tag(name = "Workspace invitations", description = "Joining a workspace an ADMIN invited you to")
public interface TenantInvitationApi {

	@GetMapping("/api/public/tenant-invitations/{token}")
	@Operation(summary = "Look at a workspace invitation", description = "Public: the workspace's name and type and the invited address, so the join page can show them before signing in.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitation returned"),
			@ApiResponse(responseCode = "404", description = "Unknown or revoked link") })
	ResponseEntity<TenantInvitationPreviewDto> preview(@PathVariable String token);

	@PostMapping("/api/me/tenant-invitations/{token}/accept")
	@Operation(summary = "Accept a workspace invitation", description = "Moves the signed-in user into the invited workspace with the invited role (an ADMIN stays one). A workspace the user had before keeps its content, but the user no longer sees it. The invitation must be for the user's verified email. Accepting again is a no-op.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Joined"),
			@ApiResponse(responseCode = "401", description = "Not signed in"),
			@ApiResponse(responseCode = "403", description = "Another or an unverified email"),
			@ApiResponse(responseCode = "404", description = "Unknown or revoked link"),
			@ApiResponse(responseCode = "409", description = "Accepted by someone else, or the personal workspace has another owner") })
	ResponseEntity<TenantJoinedDto> accept(@PathVariable String token);
}
