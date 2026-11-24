package com.knowgauge.core.service.tenant;

import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.User;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.service.settings.TenantSettingsService.TenantSettingValue;

/**
 * The current organization workspace as its tenant admins see it (ADR016): the people in it, the invitations that
 * bring more (as tenant admin, user or viewer) and its configuration (settings of type {@code CONFIG}). Its limits are neither shown nor changed here: only an {@code ADMIN} sets them,
 * on the Workspaces page (later a billing plan will). Every method throws
 * {@link com.knowgauge.core.exception.TenantAccessDeniedException} when the current workspace is not an
 * organization.
 */
public interface OrganizationTenantService {

	/** @return the users of the current workspace, by name */
	List<User> users();

	/** @return the workspace's invitations, oldest first */
	List<TenantInvitation> invitations();

	/**
	 * Invites people to the workspace with a role; an address invited before gets its link again (and the new role),
	 * one that already joined is returned as it is.
	 *
	 * @param role {@link Role#TENANT_ADMIN}, {@link Role#TENANT_USER} or {@link Role#TENANT_VIEWER}
	 */
	List<TenantInvitation> invite(List<String> emails, Role role, String personalMessage);

	/** Sends a pending invitation again, with the same link. */
	TenantInvitation resendInvitation(Long invitationId);

	/** Deletes an invitation nobody accepted yet; its link stops working. */
	void revokeInvitation(Long invitationId);

	/** @return the workspace's configuration: the CONFIG settings it may have a value of its own for */
	List<TenantSettingValue> settings();

	/**
	 * Saves the workspace's own configuration values. All are checked first, so one that does not fit saves nothing.
	 *
	 * @param values new values by key; an empty value removes the workspace's own value, so the system value applies
	 * @return the workspace's configuration afterwards
	 * @throws com.knowgauge.core.exception.TenantAccessDeniedException for a limit, or while viewing another
	 *                                                                     user's workspace
	 * @throws IllegalArgumentException                                     naming every value that does not fit
	 */
	List<TenantSettingValue> saveSettings(Map<String, String> values);

	/**
	 * Removes the workspace's own configuration values (not its limits), so the system values apply again.
	 *
	 * @return the workspace's configuration afterwards
	 */
	List<TenantSettingValue> resetSettings();
}
