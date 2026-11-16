package com.knowgauge.core.service.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.enums.AttemptStatus;
import com.knowgauge.core.port.notification.ResultMailer;
import com.knowgauge.core.port.notification.ResultMailer.ResultMail;
import com.knowgauge.core.service.invitation.InvitationProperties;

class AttemptResultNotifierTest {

	private ResultMailer mailer;
	private AttemptResultNotifier notifier;

	@BeforeEach
	void setUp() {
		mailer = mock(ResultMailer.class);
		InvitationProperties properties = new InvitationProperties();
		properties.setLinkBaseUrl("https://tests.example.com");
		notifier = new AttemptResultNotifier(mailer, properties);
	}

	@Test
	void sendsTheScoreAndTheResultsLinkToTheParticipant() {
		notifier.onScored(new AttemptScoredEvent(scored("ana@example.com"), "Safety basics"));

		ArgumentCaptor<ResultMail> mail = ArgumentCaptor.forClass(ResultMail.class);
		verify(mailer).send(mail.capture());
		assertThat(mail.getValue().to()).isEqualTo("ana@example.com");
		assertThat(mail.getValue().participantName()).isEqualTo("Ana");
		assertThat(mail.getValue().testName()).isEqualTo("Safety basics");
		assertThat(mail.getValue().correctCount()).isEqualTo(8);
		assertThat(mail.getValue().totalQuestions()).isEqualTo(10);
		assertThat(mail.getValue().scorePercent()).isEqualTo(80.0);
		assertThat(mail.getValue().resultsLink()).isEqualTo("https://tests.example.com/r/attempt-token");
	}

	@Test
	void sendsNothingForAttemptsWithoutAnEmail() {
		notifier.onScored(new AttemptScoredEvent(scored(null), "Safety basics"));

		verify(mailer, never()).send(any());
	}

	@Test
	void aMailFailureIsOnlyLogged() {
		doThrow(new MailDeliveryException("Connection refused", null)).when(mailer).send(any());

		notifier.onScored(new AttemptScoredEvent(scored("ana@example.com"), "Safety basics"));

		verify(mailer).send(any());
	}

	private static Attempt scored(String email) {
		return Attempt.builder().id(42L).testId(5L).participantName("Ana").participantEmail(email)
				.publicToken("attempt-token").status(AttemptStatus.SCORED).totalQuestions(10).correctCount(8)
				.wrongCount(2).scorePercent(80.0).build();
	}
}
