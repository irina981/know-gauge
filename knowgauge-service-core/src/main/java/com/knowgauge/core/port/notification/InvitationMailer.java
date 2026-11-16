package com.knowgauge.core.port.notification;

/**
 * Sends invitation emails (ADR009). Implemented by an infra mail adapter.
 */
public interface InvitationMailer {

	/**
	 * @param to              recipient address
	 * @param testName        name of the test
	 * @param testDescription optional description of the test
	 * @param questionCount   number of questions
	 * @param link            the recipient's personal link
	 * @param personalMessage optional note from the sender
	 */
	record InvitationMail(String to, String testName, String testDescription, int questionCount, String link,
			String personalMessage) {
	}

	/**
	 * @throws com.knowgauge.core.exception.MailDeliveryException if the mail server rejects or cannot be reached
	 */
	void send(InvitationMail mail);
}
