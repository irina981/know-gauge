package com.knowgauge.core.port.notification;

import com.knowgauge.core.model.enums.TenantType;

/**
 * Sends invitations to join a workspace (ADR015). Implemented by an infra mail adapter.
 */
public interface TenantInvitationMailer {

	/**
	 * @param to              recipient address
	 * @param tenantName   name of the workspace
	 * @param tenantType   {@link TenantType#PERSONAL} or {@link TenantType#ORGANIZATION}
	 * @param link            the recipient's link to accept
	 * @param personalMessage optional note from the admin
	 */
	record TenantInvitationMail(String to, String tenantName, TenantType tenantType, String link,
			String personalMessage) {
	}

	/**
	 * @throws com.knowgauge.core.exception.MailDeliveryException if the mail server rejects or cannot be reached
	 */
	void send(TenantInvitationMail mail);
}
