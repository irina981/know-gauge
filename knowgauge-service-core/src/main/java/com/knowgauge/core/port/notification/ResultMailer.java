package com.knowgauge.core.port.notification;

/**
 * Sends participants their result once an attempt is scored (ADR009).
 * Implemented by an infra mail adapter.
 */
public interface ResultMailer {

	/**
	 * @param to              participant's email
	 * @param participantName name the participant entered
	 * @param testName        name of the test
	 * @param correctCount    correctly answered questions
	 * @param totalQuestions  questions in the test
	 * @param scorePercent    score in percent, one decimal
	 * @param resultsLink     link to the participant's results page
	 */
	record ResultMail(String to, String participantName, String testName, int correctCount, int totalQuestions,
			double scorePercent, String resultsLink) {
	}

	/**
	 * @throws com.knowgauge.core.exception.MailDeliveryException if the mail server rejects or cannot be reached
	 */
	void send(ResultMail mail);
}
