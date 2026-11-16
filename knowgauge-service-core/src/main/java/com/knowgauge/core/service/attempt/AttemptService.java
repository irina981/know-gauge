package com.knowgauge.core.service.attempt;

import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.OptionLetter;

/**
 * Taking published tests: start an attempt, submit the answers (scored at once)
 * and list attempts. Only {@code PUBLISHED} tests accept attempts (ADR005).
 * Participants reach a test through its public token and identify themselves by
 * name and email; their attempt is then addressed by its own token (ADR009).
 */
public interface AttemptService {

	/**
	 * An attempt with what a client needs to show it.
	 *
	 * @param questions the questions in order, with the version the attempt uses
	 * @param answers   the scored answers; empty until the attempt is submitted
	 */
	record AttemptView(Attempt attempt, Test test, List<TestQuestion> questions, List<AttemptAnswer> answers) {
	}

	// ---------------- public (participants, ADR009) ----------------

	/**
	 * @return the test behind a public link
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException if the token is unknown or the test is not
	 *                                                                published (the link only works while it is)
	 */
	Test getPublishedTest(String testToken);

	/**
	 * Starts an attempt for a participant who opened the public link.
	 *
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException if the link does not lead to a published test
	 * @throws IllegalArgumentException                              if the name or email is missing or invalid
	 */
	AttemptView startForParticipant(String testToken, String participantName, String participantEmail);

	/**
	 * Starts the one attempt of a personal invitation. The email comes from the
	 * invitation and cannot be changed; only the name is entered. Opening the link
	 * again while the attempt is unfinished returns that attempt.
	 *
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException if the link is unknown or the test is not
	 *                                                                published
	 * @throws com.knowgauge.core.exception.AttemptConflictException  if the invitation was already used
	 * @throws IllegalArgumentException                              if the name is missing or invalid
	 */
	AttemptView startForInvitation(String invitationToken, String participantName);

	/** @throws com.knowgauge.core.exception.ResourceNotFoundException for an unknown token */
	AttemptView getByToken(String attemptToken);

	/** Same as {@link #submit(Long, Map)}, for the participant's attempt token. */
	AttemptView submitByToken(String attemptToken, Map<Long, List<OptionLetter>> answers);

	// ---------------- admin ----------------

	/**
	 * Starts an attempt on a published test for the current (logged-in) user.
	 *
	 * @throws com.knowgauge.core.exception.ResourceNotFoundException if the test does not exist
	 * @throws com.knowgauge.core.exception.AttemptConflictException  if the test is not published or has no questions
	 */
	AttemptView start(Long testId);

	AttemptView get(Long attemptId);

	/**
	 * Saves the answers and scores the attempt. Questions missing from
	 * {@code answers} count as unanswered (wrong). A question is correct only if
	 * the chosen options exactly match its answer key.
	 *
	 * @param answers chosen options per question id
	 * @throws com.knowgauge.core.exception.AttemptConflictException if the attempt was already submitted
	 * @throws IllegalArgumentException                              for questions that are not in the test, or
	 *                                                               several options on a single-answer test
	 */
	AttemptView submit(Long attemptId, Map<Long, List<OptionLetter>> answers);

	/**
	 * Attempts of the tenant, newest first. All filters are optional.
	 *
	 * @param participantEmail matched case-insensitively
	 */
	List<Attempt> list(Long testId, Long userId, String participantEmail);
}
