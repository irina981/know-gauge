package com.knowgauge.core.service.invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.model.enums.AttemptStatus;
import com.knowgauge.core.port.notification.InvitationMailer;
import com.knowgauge.core.port.notification.InvitationMailer.InvitationMail;
import com.knowgauge.core.service.invitation.InvitationService.InvitationView;
import com.knowgauge.core.service.invitation.InvitationTransactionalServiceImpl.Prepared;

class InvitationServiceImplTest {

	private static final long TENANT = 1L;
	private static final long TEST_ID = 5L;

	private InvitationTransactionalServiceImpl transactional;
	private InvitationMailer mailer;
	private InvitationServiceImpl service;
	private final com.knowgauge.core.model.Test test = com.knowgauge.core.model.Test.builder().id(TEST_ID)
			.tenantId(TENANT).name("Safety basics").build();

	@BeforeEach
	void setUp() {
		transactional = mock(InvitationTransactionalServiceImpl.class);
		mailer = mock(InvitationMailer.class);
		ExecutionContext context = mock(ExecutionContext.class);
		when(context.tenantId()).thenReturn(TENANT);
		InvitationProperties properties = new InvitationProperties();
		properties.setLinkBaseUrl("https://tests.example.com/");
		properties.setMaxPerRequest(3);
		service = new InvitationServiceImpl(transactional, mailer, properties, context);
		when(transactional.recordSend(eq(TENANT), anyLong(), any())).thenAnswer(inv -> {
			TestInvitation updated = TestInvitation.builder().id(inv.getArgument(1)).sendCount(1)
					.sendError(inv.getArgument(2)).build();
			return updated;
		});
	}

	@Test
	void invitesEachAddressOnceAndSendsItsOwnLink() {
		givenPrepared(Map.of(), invitation(1L, "ana@example.com", "tok-a"), invitation(2L, "marko@example.com", "tok-m"));

		List<InvitationView> result = service.invite(TEST_ID,
				List.of(" Ana@Example.com", "marko@example.com", "ana@example.com ", ""), "  Good luck!  ");

		ArgumentCaptor<List<String>> emails = listCaptor();
		verify(transactional).prepare(eq(TENANT), eq(TEST_ID), emails.capture());
		assertThat(emails.getValue()).containsExactly("ana@example.com", "marko@example.com");

		ArgumentCaptor<InvitationMail> mails = ArgumentCaptor.forClass(InvitationMail.class);
		verify(mailer, org.mockito.Mockito.times(2)).send(mails.capture());
		assertThat(mails.getAllValues()).extracting(InvitationMail::link)
				.containsExactly("https://tests.example.com/i/tok-a", "https://tests.example.com/i/tok-m");
		assertThat(mails.getAllValues()).extracting(InvitationMail::personalMessage).containsOnly("Good luck!");
		assertThat(result).hasSize(2);
	}

	@Test
	void aFailedSendIsRecordedAndDoesNotStopTheOthers() {
		givenPrepared(Map.of(), invitation(1L, "ana@example.com", "tok-a"), invitation(2L, "marko@example.com", "tok-m"));
		doThrow(new MailDeliveryException("Connection refused", null)).when(mailer).send(org.mockito.ArgumentMatchers
				.<InvitationMail>argThat(m -> m != null && m.to().equals("ana@example.com")));

		List<InvitationView> result = service.invite(TEST_ID, List.of("ana@example.com", "marko@example.com"), null);

		verify(transactional).recordSend(TENANT, 1L, "Connection refused");
		verify(transactional).recordSend(eq(TENANT), eq(2L), isNull());
		assertThat(result).extracting(v -> v.invitation().getSendError()).containsExactly("Connection refused", null);
	}

	@Test
	void usedLinksAreNotSentAgain() {
		Attempt used = Attempt.builder().id(42L).invitationId(1L).status(AttemptStatus.SCORED).build();
		givenPrepared(Map.of(1L, used), invitation(1L, "ana@example.com", "tok-a"));

		List<InvitationView> result = service.invite(TEST_ID, List.of("ana@example.com"), null);

		verify(mailer, never()).send(any());
		assertThat(result.get(0).attempt()).isSameAs(used);
	}

	@Test
	void rejectsInvalidAddressesBeforeCreatingAnything() {
		assertThatThrownBy(() -> service.invite(TEST_ID, List.of("ana@example.com", "nope", "x@y"), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nope, x@y");
		verify(transactional, never()).prepare(anyLong(), any(), anyList());
	}

	@Test
	void rejectsAnEmptyListAndTooManyAddresses() {
		assertThatThrownBy(() -> service.invite(TEST_ID, List.of(" ", ""), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
		assertThatThrownBy(() -> service.invite(TEST_ID,
				List.of("a@example.com", "b@example.com", "c@example.com", "d@example.com"), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("At most 3");
	}

	private void givenPrepared(Map<Long, Attempt> attempts, TestInvitation... invitations) {
		when(transactional.prepare(eq(TENANT), eq(TEST_ID), anyList()))
				.thenReturn(new Prepared(test, 10, List.of(invitations), attempts));
	}

	private static TestInvitation invitation(long id, String email, String token) {
		return TestInvitation.builder().id(id).tenantId(TENANT).testId(TEST_ID).email(email).publicToken(token)
				.build();
	}

	@SuppressWarnings("unchecked")
	private static ArgumentCaptor<List<String>> listCaptor() {
		return ArgumentCaptor.forClass(List.class);
	}
}
