package com.knowgauge.core.service.invitation;

import java.util.List;

import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestInvitation;

/**
 * Personal invitations to a published test (ADR009): every email gets its own
 * link, sent by the server, usable for one attempt. The attempt takes the
 * invited address as the participant's email; it cannot be changed.
 */
public interface InvitationService {

	/**
	 * An invitation with the attempt started from it, if any.
	 *
	 * @param attempt {@code null} while the link is unused
	 */
	record InvitationView(TestInvitation invitation, Attempt attempt) {
	}

	/** What the invitation link shows before the participant starts. */
	record PublicInvitationView(TestInvitation invitation, Test test, int questionCount, Attempt attempt) {
	}

	/**
	 * Invites each address and sends its personal link. Addresses that were
	 * already invited keep their link and get it again, unless it was used.
	 * Delivery failures do not fail the request; they are stored on the
	 * invitation ({@code sendError}) so it can be resent.
	 *
	 * @param emails          addresses; duplicates (ignoring case) are merged
	 * @param personalMessage optional note added to the email
	 * @return the invitations of the given addresses, in the given order
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException if the test does not exist
	 * @throws com.knowgauge.core.exception.AttemptConflictException  if the test is not published
	 * @throws IllegalArgumentException                              for no, too many or invalid addresses
	 */
	List<InvitationView> invite(Long testId, List<String> emails, String personalMessage);

	/**
	 * Sends the personal link again.
	 *
	 * @throws com.knowgauge.core.exception.AttemptConflictException if the link was already used or the test is not
	 *                                                               published
	 */
	InvitationView resend(Long testId, Long invitationId, String personalMessage);

	/** @return the invitations of a test, oldest first */
	List<InvitationView> list(Long testId);

	/**
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException if the token is unknown or the test is not
	 *                                                                published
	 */
	PublicInvitationView getByToken(String invitationToken);
}
