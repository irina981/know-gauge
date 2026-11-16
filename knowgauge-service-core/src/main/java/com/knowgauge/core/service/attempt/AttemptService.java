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

	/**
	 * Starts an attempt on a published test for the current user.
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
	 * Attempts of the tenant, newest first.
	 *
	 * @param testId optional filter
	 * @param userId optional filter
	 */
	List<Attempt> list(Long testId, Long userId);
}
