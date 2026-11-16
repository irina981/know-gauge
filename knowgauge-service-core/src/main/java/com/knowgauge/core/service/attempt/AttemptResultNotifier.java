package com.knowgauge.core.service.attempt;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.port.notification.ResultMailer;
import com.knowgauge.core.port.notification.ResultMailer.ResultMail;
import com.knowgauge.core.service.invitation.InvitationProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Emails participants their score and a link to their results page (ADR009).
 * Runs after the scoring transaction commits, so mail never holds a DB
 * transaction open and a mail failure never undoes or fails the submit.
 */
@Slf4j
@Component
public class AttemptResultNotifier {

	private final ResultMailer mailer;
	private final InvitationProperties properties;

	public AttemptResultNotifier(ResultMailer mailer, InvitationProperties properties) {
		this.mailer = mailer;
		this.properties = properties;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onScored(AttemptScoredEvent event) {
		Attempt attempt = event.attempt();
		if (attempt.getParticipantEmail() == null) {
			return; // taken by an administrator through the admin API
		}
		try {
			mailer.send(new ResultMail(attempt.getParticipantEmail(), attempt.getParticipantName(), event.testName(),
					attempt.getCorrectCount(), attempt.getTotalQuestions(), attempt.getScorePercent(),
					properties.resultsLinkFor(attempt.getPublicToken())));
			log.info("Attempt {} on test {} - result sent to {}", attempt.getId(), attempt.getTestId(),
					attempt.getParticipantEmail());
		} catch (RuntimeException e) {
			log.warn("Attempt {} on test {} - result could not be sent to {}: {}", attempt.getId(),
					attempt.getTestId(), attempt.getParticipantEmail(), e.getMessage());
		}
	}
}
