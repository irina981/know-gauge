package com.knowgauge.core.port.notification;

import com.knowgauge.core.model.enums.TenantType;

/**
 * Sends invitations to join a workspace (ADR015). Implemented by an infra mail adapter.
 */
public interface WorkspaceInvitationMailer {

	/**
	 * @param to              recipient address
	 * @param workspaceName   name of the workspace
	 * @param workspaceType   {@link TenantType#PERSONAL} or {@link TenantType#ORGANIZATION}
	 * @param link            the recipient's link to accept
	 * @param personalMessage optional note from the admin
	 */
	record WorkspaceInvitationMail(String to, String workspaceName, TenantType workspaceType, String link,
			String personalMessage) {
	}

	/**
	 * @throws com.knowgauge.core.exception.MailDeliveryException if the mail server rejects or cannot be reached
	 */
	void send(WorkspaceInvitationMail mail);
}
