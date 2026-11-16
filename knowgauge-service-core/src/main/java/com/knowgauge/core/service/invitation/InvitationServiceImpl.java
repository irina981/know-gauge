package com.knowgauge.core.service.invitation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.port.notification.InvitationMailer;
import com.knowgauge.core.port.notification.InvitationMailer.InvitationMail;
import com.knowgauge.core.service.invitation.InvitationTransactionalServiceImpl.Prepared;
import com.knowgauge.core.util.Emails;

import lombok.extern.slf4j.Slf4j;

/**
 * Non-transactional: mail goes out between the short transactions of
 * {@link InvitationTransactionalServiceImpl}, never inside one.
 */
@Slf4j
@Service
public class InvitationServiceImpl implements InvitationService {

	private static final int MAX_MESSAGE_LENGTH = 2000;

	private final InvitationTransactionalServiceImpl transactionalService;
	private final InvitationMailer mailer;
	private final InvitationProperties properties;
	private final ExecutionContext executionContext;

	public InvitationServiceImpl(InvitationTransactionalServiceImpl transactionalService, InvitationMailer mailer,
			InvitationProperties properties, ExecutionContext executionContext) {
		this.transactionalService = transactionalService;
		this.mailer = mailer;
		this.properties = properties;
		this.executionContext = executionContext;
	}

	@Override
	public List<InvitationView> invite(Long testId, List<String> emails, String personalMessage) {
		List<String> addresses = normalizeAll(emails);
		String message = normalizeMessage(personalMessage);
		long tenantId = executionContext.tenantId();
		Prepared prepared = transactionalService.prepare(tenantId, testId, addresses);

		List<InvitationView> result = new ArrayList<>(prepared.invitations().size());
		int sent = 0;
		for (TestInvitation invitation : prepared.invitations()) {
			Attempt attempt = prepared.attemptsByInvitation().get(invitation.getId());
			if (attempt != null) {
				// A used link is not sent again; the result shows its attempt instead.
				result.add(new InvitationView(invitation, attempt));
				continue;
			}
			TestInvitation updated = send(tenantId, prepared, invitation, message);
			if (updated.getSendError() == null) {
				sent++;
			}
			result.add(new InvitationView(updated, null));
		}
		log.info("Test {} - invited {} addresses, {} emails sent", testId, addresses.size(), sent);
		return result;
	}

	@Override
	public InvitationView resend(Long testId, Long invitationId, String personalMessage) {
		String message = normalizeMessage(personalMessage);
		long tenantId = executionContext.tenantId();
		Prepared prepared = transactionalService.prepareResend(tenantId, testId, invitationId);
		return new InvitationView(send(tenantId, prepared, prepared.invitations().get(0), message), null);
	}

	@Override
	public List<InvitationView> list(Long testId) {
		return transactionalService.list(executionContext.tenantId(), testId);
	}

	@Override
	public PublicInvitationView getByToken(String invitationToken) {
		return transactionalService.getByToken(executionContext.tenantId(), invitationToken);
	}

	private TestInvitation send(long tenantId, Prepared prepared, TestInvitation invitation, String message) {
		String error = null;
		try {
			mailer.send(new InvitationMail(invitation.getEmail(), prepared.test().getName(),
					prepared.test().getDescription(), prepared.questionCount(),
					properties.linkFor(invitation.getPublicToken()), message));
		} catch (MailDeliveryException e) {
			error = e.getMessage();
			log.warn("Test {} - invitation {} to {} could not be sent: {}", prepared.test().getId(),
					invitation.getId(), invitation.getEmail(), error);
		}
		return transactionalService.recordSend(tenantId, invitation.getId(), error);
	}

	private List<String> normalizeAll(List<String> emails) {
		Set<String> addresses = new LinkedHashSet<>();
		List<String> invalid = new ArrayList<>();
		if (emails != null) {
			for (String email : emails) {
				if (email == null || email.isBlank()) {
					continue;
				}
				String normalized = Emails.normalizeOrNull(email);
				if (normalized == null) {
					invalid.add(email.trim());
				} else {
					addresses.add(normalized);
				}
			}
		}
		if (!invalid.isEmpty()) {
			throw new IllegalArgumentException("These are not valid email addresses: " + String.join(", ", invalid));
		}
		if (addresses.isEmpty()) {
			throw new IllegalArgumentException("Enter at least one email address.");
		}
		if (addresses.size() > properties.getMaxPerRequest()) {
			throw new IllegalArgumentException("At most " + properties.getMaxPerRequest()
					+ " addresses can be invited at once, but " + addresses.size() + " were given.");
		}
		return List.copyOf(addresses);
	}

	private static String normalizeMessage(String message) {
		if (message == null || message.isBlank()) {
			return null;
		}
		String trimmed = message.trim();
		if (trimmed.length() > MAX_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("The message can be at most " + MAX_MESSAGE_LENGTH + " characters.");
		}
		return trimmed;
	}
}
