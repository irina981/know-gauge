package com.knowgauge.core.service.attempt;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.ParticipantLimitReachedException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.core.port.repository.TestInvitationRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;

/**
 * The participants of a test and its workspace limit "Participants per test" (ADR014): the different email
 * addresses invited to the test or that started an attempt (personal invitation or shared link), each counted once.
 * Attempts of signed-in users without an email (an admin trying the test) do not count. Callers lock the test row
 * first, so two requests cannot both take the last place.
 */
@Component
public class TestParticipants {

	private final TestInvitationRepository invitationRepository;
	private final AttemptRepository attemptRepository;
	private final TenantSettingsService tenantSettings;

	public TestParticipants(TestInvitationRepository invitationRepository, AttemptRepository attemptRepository,
			TenantSettingsService tenantSettings) {
		this.invitationRepository = invitationRepository;
		this.attemptRepository = attemptRepository;
		this.tenantSettings = tenantSettings;
	}

	/** @return the participants' addresses, lower-cased */
	public Set<String> emails(long tenantId, Long testId) {
		Set<String> emails = new HashSet<>();
		for (TestInvitation invitation : invitationRepository.findByTenantIdAndTestId(tenantId, testId)) {
			emails.add(normalize(invitation.getEmail()));
		}
		for (Attempt attempt : attemptRepository.findByTenantId(tenantId, testId, null, null)) {
			if (attempt.getParticipantEmail() != null) {
				emails.add(normalize(attempt.getParticipantEmail()));
			}
		}
		emails.remove(null);
		return emails;
	}

	/**
	 * Inviting: refuses addresses that would make the test go over its limit. Addresses that already take part
	 * never count twice.
	 *
	 * @param addresses lower-cased
	 * @throws ParticipantLimitReachedException naming how many more fit
	 */
	public void requireRoomForInvitations(long tenantId, Long testId, Collection<String> addresses) {
		Integer limit = tenantSettings.limits(tenantId).participantsPerTest();
		if (limit == null) {
			return;
		}
		Set<String> current = emails(tenantId, testId);
		Set<String> added = new LinkedHashSet<>(addresses);
		added.removeAll(current);
		if (current.size() + added.size() > limit) {
			int room = Math.max(0, limit - current.size());
			throw new ParticipantLimitReachedException("This test can have at most " + participants(limit) + " and has "
					+ current.size() + ", so " + (room == 0 ? "no one else" : room + " more") + " can be invited, not "
					+ added.size() + ".");
		}
	}

	/**
	 * Shared link: refuses someone new once the test is full. People who already take part may start again.
	 *
	 * @param email lower-cased
	 * @throws ParticipantLimitReachedException when the test is full
	 */
	public void requireRoomForParticipant(long tenantId, Long testId, String email) {
		Integer limit = tenantSettings.limits(tenantId).participantsPerTest();
		if (limit == null) {
			return;
		}
		Set<String> current = emails(tenantId, testId);
		if (!current.contains(normalize(email)) && current.size() >= limit) {
			throw new ParticipantLimitReachedException(
					"This test is full: it can have at most " + participants(limit) + ". Please contact the person who"
							+ " shared the link.");
		}
	}

	private static String participants(int count) {
		return count + (count == 1 ? " participant" : " participants");
	}

	private static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
