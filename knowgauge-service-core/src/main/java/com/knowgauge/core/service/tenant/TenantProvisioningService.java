package com.knowgauge.core.service.tenant;

import java.util.List;

import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;

/**
 * An {@code ADMIN} creates workspaces of any type and invites people to them by email (ADR015). Invitees become
 * {@code TENANT_ADMIN} of the workspace when they accept, unless the invitation names another role (ADR016).
 */
public interface TenantProvisioningService {

	/** A new workspace and the invitations sent for it (a failed send is kept with its error). */
	record CreatedTenant(TenantSummary tenant, List<TenantInvitation> invitations) {
	}

	/**
	 * Creates a workspace: a personal one with exactly one invitee (its owner-to-be), an organization with at least
	 * one, a shared library with none (it has no members and no limits).
	 *
	 * @throws IllegalArgumentException for a missing name or type, invalid addresses, or the wrong number of invitees
	 */
	CreatedTenant create(TenantType type, String name, List<String> emails, String personalMessage);

	/**
	 * Invites more people as {@link Role#TENANT_ADMIN}: any number to an organization, one to a personal workspace
	 * that has no owner yet. An address invited before gets its link again; one that already joined is returned as
	 * it is.
	 */
	default List<TenantInvitation> invite(Long tenantId, List<String> emails, String personalMessage) {
		return invite(tenantId, emails, Role.TENANT_ADMIN, personalMessage);
	}

	/**
	 * Invites more people with the given role (ADR016); a pending invitation to the same address gets the new role.
	 *
	 * @param role {@link Role#TENANT_ADMIN}, {@link Role#TENANT_USER} or {@link Role#TENANT_VIEWER}; a personal
	 *             workspace takes only {@link Role#TENANT_ADMIN}
	 * @throws IllegalArgumentException for another role, or as {@link #invite(Long, List, String)}
	 */
	List<TenantInvitation> invite(Long tenantId, List<String> emails, Role role, String personalMessage);

	/** @return the workspace's invitations, oldest first */
	List<TenantInvitation> listInvitations(Long tenantId);

	TenantInvitation resend(Long tenantId, Long invitationId, String personalMessage);

	/** Deletes an invitation nobody accepted yet; its link stops working. */
	void revoke(Long tenantId, Long invitationId);
}
