package com.knowgauge.core.service.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.exception.ParticipantLimitReachedException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.core.port.repository.TestInvitationRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;

/** The workspace limit "Participants per test": invited or started an attempt, each address counted once. */
class TestParticipantsTest {

	private static final long TENANT = 5L;
	private static final Long TEST = 100L;

	private TestInvitationRepository invitations;
	private AttemptRepository attempts;
	private TenantSettingsService settings;
	private TestParticipants participants;

	@BeforeEach
	void setUp() {
		invitations = mock(TestInvitationRepository.class);
		attempts = mock(AttemptRepository.class);
		settings = mock(TenantSettingsService.class);
		participants = new TestParticipants(invitations, attempts, settings);
		// Ana was invited and also started an attempt; Marko came through the shared link; the admin's own try has no
		// email and does not count.
		when(invitations.findByTenantIdAndTestId(TENANT, TEST)).thenReturn(
				List.of(TestInvitation.builder().email("ana@example.com").build()));
		when(attempts.findByTenantId(TENANT, TEST, null, null)).thenReturn(List.of(
				Attempt.builder().participantEmail("Ana@Example.com").build(),
				Attempt.builder().participantEmail("marko@example.com").build(), Attempt.builder().userId(1L).build()));
	}

	private void limit(Integer participantsPerTest) {
		when(settings.limits(TENANT)).thenReturn(new TenantLimits(null, null, null, participantsPerTest));
	}

	@Test
	void everyAddressCountsOnce() {
		assertThat(participants.emails(TENANT, TEST)).containsExactlyInAnyOrder("ana@example.com", "marko@example.com");
	}

	@Test
	void invitingOverTheLimitSaysHowManyMoreFit() {
		limit(3);

		// Ana already takes part, so only one address is new: it fits.
		assertThatCode(() -> participants.requireRoomForInvitations(TENANT, TEST,
				List.of("ana@example.com", "petra@example.com"))).doesNotThrowAnyException();
		assertThatThrownBy(() -> participants.requireRoomForInvitations(TENANT, TEST,
				List.of("petra@example.com", "ivan@example.com")))
				.isInstanceOf(ParticipantLimitReachedException.class).hasMessageContaining("at most 3")
				.hasMessageContaining("1 more");
	}

	@Test
	void aFullTestRefusesNewPeopleButNotThoseAlreadyIn() {
		limit(2);

		assertThatCode(() -> participants.requireRoomForParticipant(TENANT, TEST, "marko@example.com"))
				.doesNotThrowAnyException();
		assertThatThrownBy(() -> participants.requireRoomForParticipant(TENANT, TEST, "petra@example.com"))
				.isInstanceOf(ParticipantLimitReachedException.class).hasMessageContaining("full");
	}

	@Test
	void noLimitAllowsAnything() {
		limit(null);

		assertThatCode(() -> participants.requireRoomForParticipant(TENANT, TEST, "petra@example.com"))
				.doesNotThrowAnyException();
		assertThatCode(() -> participants.requireRoomForInvitations(TENANT, TEST, List.of("a@x.com", "b@x.com")))
				.doesNotThrowAnyException();
	}
}
