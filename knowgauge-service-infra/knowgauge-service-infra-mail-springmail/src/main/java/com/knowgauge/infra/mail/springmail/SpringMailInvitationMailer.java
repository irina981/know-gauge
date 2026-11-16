package com.knowgauge.infra.mail.springmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.port.notification.InvitationMailer;

/**
 * Sends invitations as plain-text mail through the SMTP server configured
 * under {@code spring.mail.*} (Mailpit locally).
 */
@Component
public class SpringMailInvitationMailer implements InvitationMailer {

	private final JavaMailSender mailSender;
	private final String from;

	public SpringMailInvitationMailer(JavaMailSender mailSender, @Value("${kg.mail.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public void send(InvitationMail mail) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(mail.to());
		message.setSubject("You are invited to take the test \"" + mail.testName() + "\"");
		message.setText(body(mail));
		try {
			mailSender.send(message);
		} catch (MailException e) {
			throw new MailDeliveryException("Could not send the invitation to " + mail.to() + ": "
					+ e.getMostSpecificCause().getMessage(), e);
		}
	}

	static String body(InvitationMail mail) {
		StringBuilder text = new StringBuilder("Hello,\n\nyou are invited to take the test \"")
				.append(mail.testName()).append("\" (").append(mail.questionCount())
				.append(mail.questionCount() == 1 ? " question).\n" : " questions).\n");
		if (mail.testDescription() != null && !mail.testDescription().isBlank()) {
			text.append('\n').append(mail.testDescription().trim()).append('\n');
		}
		if (mail.personalMessage() != null) {
			text.append('\n').append(mail.personalMessage()).append('\n');
		}
		text.append("\nStart the test here:\n").append(mail.link()).append("\n\n")
				.append("This link is personal and can be used for one attempt. Please do not forward it.\n");
		return text.toString();
	}
}
