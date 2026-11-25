package com.knowgauge.contract.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.knowgauge.contract.dto.TenantCreateInput;
import com.knowgauge.contract.dto.TenantCreatedDto;
import com.knowgauge.contract.dto.TenantInvitationDto;
import com.knowgauge.contract.dto.TenantInviteInput;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** An ADMIN creates workspaces and invites people to them (ADR015). Under /api/admin: ADMIN role only. */
@Tag(name = "Admin", description = "Application administration (ADMIN role)")
public interface TenantAdminApi {

	@PostMapping("/api/admin/tenants")
	@Operation(summary = "Create a workspace", description = "Creates a workspace (tenant) of any type and emails its invitations: a PERSONAL one needs exactly one address (its owner once they accept), an ORGANIZATION at least one, a LIBRARY none (it has no members and no limits; every user reads it). Invitees become TENANT_ADMIN of the workspace when they accept; a failed send is kept on the invitation and can be resent.")
	@ApiResponses({ @ApiResponse(responseCode = "201", description = "Workspace created, invitations sent"),
			@ApiResponse(responseCode = "400", description = "Missing name or type, invalid address, or the wrong number of invitees for the type"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN") })
	ResponseEntity<TenantCreatedDto> create(@Valid @RequestBody TenantCreateInput input);

	@GetMapping("/api/admin/tenants/{id}/invitations")
	@Operation(summary = "List a workspace's invitations", description = "Oldest first, with whether each was delivered and accepted.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitations returned"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such workspace") })
	ResponseEntity<List<TenantInvitationDto>> invitations(@PathVariable Long id);

	@PostMapping("/api/admin/tenants/{id}/invitations")
	@Operation(summary = "Invite more people to a workspace", description = "An ORGANIZATION takes any number; a PERSONAL workspace one, while it has no owner and no invitation to someone else; a LIBRARY none. An address invited before gets its link again; one that already joined is returned unchanged.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitations sent; one per address"),
			@ApiResponse(responseCode = "400", description = "Invalid address, or the wrong number for the type"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such workspace"),
			@ApiResponse(responseCode = "409", description = "The personal workspace already has its owner or an invitation to someone else") })
	ResponseEntity<List<TenantInvitationDto>> invite(@PathVariable Long id, @Valid @RequestBody TenantInviteInput input);

	@PostMapping("/api/admin/tenants/{id}/invitations/{invitationId}/resend")
	@Operation(summary = "Send a workspace invitation again", description = "Same link as before.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Sent, or the send error is on the invitation"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such workspace or invitation"),
			@ApiResponse(responseCode = "409", description = "Already accepted") })
	ResponseEntity<TenantInvitationDto> resend(@PathVariable Long id, @PathVariable Long invitationId);

	@DeleteMapping("/api/admin/tenants/{id}/invitations/{invitationId}")
	@Operation(summary = "Revoke a workspace invitation", description = "Deletes an invitation nobody accepted yet; its link stops working.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Revoked"),
			@ApiResponse(responseCode = "403", description = "Not an ADMIN"),
			@ApiResponse(responseCode = "404", description = "No such workspace or invitation"),
			@ApiResponse(responseCode = "409", description = "Already accepted") })
	ResponseEntity<Void> revoke(@PathVariable Long id, @PathVariable Long invitationId);
}
