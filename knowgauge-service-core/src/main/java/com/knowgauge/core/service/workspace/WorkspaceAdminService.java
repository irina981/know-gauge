package com.knowgauge.core.service.workspace;

import java.util.List;

import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.model.enums.TenantType;

/**
 * An {@code ADMIN} creates workspaces of any type and invites people to them by email (ADR015). Invitees become
 * {@code TENANT_ADMIN} of the workspace when they accept.
 */
public interface WorkspaceAdminService {

	/** A new workspace and the invitations sent for it (a failed send is kept with its error). */
	record CreatedWorkspace(TenantSummary tenant, List<WorkspaceInvitation> invitations) {
	}

	/**
	 * Creates a workspace: a personal one with exactly one invitee (its owner-to-be), an organization with at least
	 * one, a shared library with none (it has no members and no limits).
	 *
	 * @throws IllegalArgumentException for a missing name or type, invalid addresses, or the wrong number of invitees
	 */
	CreatedWorkspace create(TenantType type, String name, List<String> emails, String personalMessage);

	/**
	 * Invites more people: any number to an organization, one to a personal workspace that has no owner yet. An
	 * address invited before gets its link again; one that already joined is returned as it is.
	 */
	List<WorkspaceInvitation> invite(Long tenantId, List<String> emails, String personalMessage);

	/** @return the workspace's invitations, oldest first */
	List<WorkspaceInvitation> listInvitations(Long tenantId);

	WorkspaceInvitation resend(Long tenantId, Long invitationId, String personalMessage);

	/** Deletes an invitation nobody accepted yet; its link stops working. */
	void revoke(Long tenantId, Long invitationId);
}
