package com.knowgauge.infra.mail.springmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.notification.TenantInvitationMailer;

/**
 * Sends workspace invitations (ADR015) as plain-text mail through the SMTP server configured under
 * {@code spring.mail.*} (Mailpit locally).
 */
@Component
public class SpringMailTenantInvitationMailer implements TenantInvitationMailer {

	private final JavaMailSender mailSender;
	private final String from;

	public SpringMailTenantInvitationMailer(JavaMailSender mailSender, @Value("${kg.mail.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public void send(TenantInvitationMail mail) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(mail.to());
		message.setSubject("You are invited to join \"" + mail.tenantName() + "\" on KnowGauge");
		message.setText(body(mail));
		try {
			mailSender.send(message);
		} catch (MailException e) {
			throw new MailDeliveryException("Could not send the invitation to " + mail.to() + ": "
					+ e.getMostSpecificCause().getMessage(), e);
		}
	}

	static String body(TenantInvitationMail mail) {
		boolean personal = mail.tenantType() == TenantType.PERSONAL;
		StringBuilder text = new StringBuilder("Hello,\n\n");
		if (personal) {
			text.append("a KnowGauge workspace has been set up for you: \"").append(mail.tenantName())
					.append("\".\n");
		} else {
			text.append("you are invited to join the organization \"").append(mail.tenantName())
					.append("\" on KnowGauge as one of its administrators.\n");
		}
		text.append("\nKnowGauge turns your PDFs into multiple-choice tests that are checked against the source.\n");
		if (mail.personalMessage() != null) {
			text.append('\n').append(mail.personalMessage()).append('\n');
		}
		text.append("\nAccept the invitation here:\n").append(mail.link()).append("\n\n")
				.append("Sign in, or create an account, with this email address (").append(mail.to())
				.append(").\nIf you already use KnowGauge, accepting moves your account to this workspace; ")
				.append("your current workspace keeps its content but you will no longer see it.\n")
				.append("\nThis link is personal. Please do not forward it.\n");
		return text.toString();
	}
}
