package com.knowgauge.infra.mail.springmail;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.port.notification.ResultMailer;

/**
 * Sends the participant's score as plain-text mail through the SMTP server
 * configured under {@code spring.mail.*} (Mailpit locally).
 */
@Component
public class SpringMailResultMailer implements ResultMailer {

	private final JavaMailSender mailSender;
	private final String from;

	public SpringMailResultMailer(JavaMailSender mailSender, @Value("${kg.mail.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	@Override
	public void send(ResultMail mail) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(mail.to());
		message.setSubject("Your result: " + percent(mail.scorePercent()) + " in \"" + mail.testName() + "\"");
		message.setText(body(mail));
		try {
			mailSender.send(message);
		} catch (MailException e) {
			throw new MailDeliveryException("Could not send the result to " + mail.to() + ": "
					+ e.getMostSpecificCause().getMessage(), e);
		}
	}

	static String body(ResultMail mail) {
		String greeting = mail.participantName() == null || mail.participantName().isBlank() ? "Hello"
				: "Hello " + mail.participantName().trim();
		return greeting + ",\n\nyou completed the test \"" + mail.testName() + "\".\n\n" + "Your score: "
				+ percent(mail.scorePercent()) + " (" + mail.correctCount() + " of " + mail.totalQuestions()
				+ " questions correct).\n\n" + "See which of your answers were right or wrong:\n" + mail.resultsLink()
				+ "\n\nThis link is personal. Please do not forward it.\n";
	}

	private static String percent(double value) {
		return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value) + "%";
	}
}
