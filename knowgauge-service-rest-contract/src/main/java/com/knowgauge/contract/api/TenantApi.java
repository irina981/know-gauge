package com.knowgauge.contract.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.knowgauge.contract.dto.TenantInvitationDto;
import com.knowgauge.contract.dto.TenantInviteInput;
import com.knowgauge.contract.dto.TenantSettingDto;
import com.knowgauge.contract.dto.TenantSettingsInput;
import com.knowgauge.contract.dto.TenantUserDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * The signed-in user's own organization workspace, for its tenant admins (ADR016). Everything under /api/tenant
 * needs the TENANT_ADMIN role; workspaces that are not organizations are refused.
 */
@Tag(name = "Workspace", description = "Users, invitations and configuration of the current organization workspace (TENANT_ADMIN role)")
public interface TenantApi {

	@GetMapping("/api/tenant/users")
	@Operation(summary = "List the workspace's users", description = "The users of the current organization workspace with their roles, by name.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Users returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace") })
	ResponseEntity<List<TenantUserDto>> tenantUsers();

	@GetMapping("/api/tenant/invitations")
	@Operation(summary = "List the workspace's invitations", description = "Oldest first, with the role each gives and whether it was delivered and accepted.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitations returned"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace") })
	ResponseEntity<List<TenantInvitationDto>> tenantInvitations();

	@PostMapping("/api/tenant/invitations")
	@Operation(summary = "Invite people to the workspace", description = "Emails each address a link to join as TENANT_ADMIN (default), TENANT_USER or TENANT_VIEWER (ADR016). An address invited before gets its link again with the new role; one that already joined is returned unchanged. Someone who accepts leaves their current workspace (ADR015).")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Invitations sent; one per address"),
			@ApiResponse(responseCode = "400", description = "Invalid address or role, or too many addresses at once"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace") })
	ResponseEntity<List<TenantInvitationDto>> inviteToTenant(@Valid @RequestBody TenantInviteInput input);

	@PostMapping("/api/tenant/invitations/{invitationId}/resend")
	@Operation(summary = "Send a workspace invitation again", description = "Same link as before.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Sent, or the send error is on the invitation"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace"),
			@ApiResponse(responseCode = "404", description = "No such invitation in this workspace"),
			@ApiResponse(responseCode = "409", description = "Already accepted") })
	ResponseEntity<TenantInvitationDto> resendTenantInvitation(@PathVariable Long invitationId);

	@DeleteMapping("/api/tenant/invitations/{invitationId}")
	@Operation(summary = "Revoke a workspace invitation", description = "Deletes an invitation nobody accepted yet; its link stops working.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Revoked"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace"),
			@ApiResponse(responseCode = "404", description = "No such invitation in this workspace"),
			@ApiResponse(responseCode = "409", description = "Already accepted") })
	ResponseEntity<Void> revokeTenantInvitation(@PathVariable Long invitationId);

	@GetMapping("/api/tenant/settings")
	@Operation(summary = "List the workspace's configuration", description = "The configuration (CONFIG settings) a workspace may have a value of its own for (ADR014): the system value, the workspace's own value and the value in effect. Its limits are not listed: only an ADMIN sets them.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Configuration returned"),
			@ApiResponse(responseCode = "401", description = "Not signed in"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace") })
	ResponseEntity<List<TenantSettingDto>> tenantSettings();

	@PutMapping("/api/tenant/settings")
	@Operation(summary = "Save the workspace's configuration", description = "Saves the workspace's own values of CONFIG settings, which override the system values for it and apply at once. An empty value removes the workspace's own value. All values are checked first: if one does not fit, nothing is saved.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Configuration saved; the workspace's configuration returned"),
			@ApiResponse(responseCode = "400", description = "A value does not fit, or the setting applies to the whole system only"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, not an organization workspace, or a limit"),
			@ApiResponse(responseCode = "404", description = "No such setting") })
	ResponseEntity<List<TenantSettingDto>> saveTenantSettings(@Valid @RequestBody TenantSettingsInput input);

	@DeleteMapping("/api/tenant/settings")
	@Operation(summary = "Reset the workspace's configuration", description = "Removes the workspace's own CONFIG values, so the system values apply to it again. Its limits stay.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Configuration reset; the workspace's configuration returned"),
			@ApiResponse(responseCode = "403", description = "Not a TENANT_ADMIN, or not an organization workspace") })
	ResponseEntity<List<TenantSettingDto>> resetTenantSettings();
}
