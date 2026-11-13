package com.knowgauge.core.service.review;

import java.util.List;

import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestEvent;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;

/**
 * Review of generated test questions and access to their history (ADR001).
 *
 * <p>
 * Every action creates a new immutable version, moves the question to it only if
 * the question is still at {@code expectedCurrentVersionId}, records an event
 * with the acting user, and recomputes the test status. Once the test is
 * published, every action is rejected.
 */
public interface TestReviewService {

	/**
	 * Replaces every flagged option's text with the verifier's suggestion. The
	 * answer key is unchanged.
	 */
	TestQuestion acceptFix(Long testId, Long questionId, Long expectedCurrentVersionId, String comment);

	/**
	 * Replaces the question with a newly generated one from the test's original
	 * context. The new version is verified again and may itself need review.
	 */
	TestQuestion regenerate(Long testId, Long questionId, Long expectedCurrentVersionId, String comment);

	/**
	 * Replaces the question with one written by the reviewer. Not LLM-verified.
	 */
	TestQuestion manualReplace(Long testId, Long questionId, ManualQuestionDraft draft);

	/**
	 * Publishes the test (ADR005): its content is frozen, no review action can
	 * change it any more, and attempts can be started. Allowed only from
	 * {@code GENERATED} or {@code REVIEWED}, with no question needing review.
	 */
	Test publish(Long testId, String comment);

	/**
	 * Returns a published test to the status it had before publishing, so it can
	 * be reviewed again (ADR005). Allowed only while no attempt exists for it.
	 */
	Test unpublish(Long testId, String comment);

	/**
	 * @return whether any attempt exists for the test. While none does, a
	 *         published test can be unpublished (ADR005).
	 */
	boolean hasAttempts(Long testId);

	/**
	 * @return all versions of the question, oldest first
	 */
	List<TestQuestionVersion> getVersions(Long testId, Long questionId);

	/**
	 * @param questionId optional filter
	 * @return the test's history trail, oldest first
	 */
	List<TestEvent> getEvents(Long testId, Long questionId);

	/**
	 * @return every LLM call made for the test, oldest first
	 */
	List<GenerationRun> getGenerationRuns(Long testId);
}
