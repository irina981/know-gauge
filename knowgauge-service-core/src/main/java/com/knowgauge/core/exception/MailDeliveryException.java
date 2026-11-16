package com.knowgauge.core.exception;

/**
 * An email could not be handed to the mail server. Invitation sending records
 * the message on the invitation instead of failing the whole request.
 */
public class MailDeliveryException extends BaseException {

	private static final long serialVersionUID = 1L;

	public MailDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}
}
