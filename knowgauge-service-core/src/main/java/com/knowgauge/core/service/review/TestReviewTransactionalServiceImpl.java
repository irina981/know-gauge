package com.knowgauge.core.service.review;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.exception.ReviewConflictException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.QuestionStatus;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.model.enums.VersionOrigin;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.history.TestHistoryRecorder;

/**
 * The single write transaction of every review action: insert the new version,
 * switch the slot to it (optimistic check), record events, recompute the test
 * status. A conflict rolls the whole thing back, including the inserted version.
 * Also publishes tests. Both lock the test row first, so a review action and a
 * publish of the same test never interleave.
 */
@Service
public class TestReviewTransactionalServiceImpl {

	private final TestRepository testRepository;
	private final TestQuestionRepository questionRepository;
	private final TestHistoryRecorder history;

	public TestReviewTransactionalServiceImpl(TestRepository testRepository, TestQuestionRepository questionRepository,
			TestHistoryRecorder history) {
		this.testRepository = testRepository;
		this.questionRepository = questionRepository;
		this.history = history;
	}

	public record VersionChange(VersionOrigin origin, TestEventType eventType, Long actorUserId, String comment,
			Map<String, Object> payload) {
	}

	@Transactional
	public TestQuestion applyNewVersion(Long tenantId, TestQuestion question, Long expectedCurrentVersionId,
			TestQuestionVersion draft, VersionChange change) {

		// Lock the test row so a concurrent publish cannot slip in between this check
		// and the commit (ADR005).
		Test test = lockReviewableTest(tenantId, question.getTestId());

		draft.setId(null);
		draft.setTenantId(tenantId);
		draft.setQuestionId(question.getId());
		draft.setParentVersionId(expectedCurrentVersionId);
		draft.setOrigin(change.origin());
		draft.setVersionNo(questionRepository.findMaxVersionNo(tenantId, question.getId()) + 1);
		draft.setCreatedBy(change.actorUserId());
		draft.setCreatedAt(Instant.now());
		TestQuestionVersion saved = questionRepository.insertVersion(draft);

		QuestionStatus newStatus = saved.hasFlags() ? QuestionStatus.NEEDS_REVIEW : QuestionStatus.OK;
		boolean switched = questionRepository.switchCurrentVersion(tenantId, question.getId(),
				expectedCurrentVersionId, saved.getId(), newStatus);
		if (!switched) {
			throw new ReviewConflictException("Question " + question.getId()
					+ " was changed by someone else. Reload it and review the current version.");
		}

		history.versionChanged(question, change.eventType(), expectedCurrentVersionId, saved, change.actorUserId(),
				change.comment(), change.payload());

		syncTestStatus(tenantId, question.getTestId(), test.getStatus(), change.actorUserId());

		return questionRepository.findByTenantIdAndId(tenantId, question.getId()).orElseThrow();
	}

	/**
	 * Freezes the test: moves it to {@code PUBLISHED}, after which no review
	 * action can change it and attempts can be started (ADR005). Allowed only
	 * from {@code GENERATED} or {@code REVIEWED}, with every question OK.
	 */
	@Transactional
	public Test publish(Long tenantId, Long testId, Long actorUserId, String comment) {
		Test test = testRepository.findByTenantIdAndIdForUpdate(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
		TestStatus current = test.getStatus();
		if (current == TestStatus.PUBLISHED) {
			throw new ReviewConflictException("Test " + testId + " is already published.");
		}
		if (current == null || !current.isPublishable()) {
			throw new ReviewConflictException("Test " + testId + " cannot be published in status " + current
					+ ". Only GENERATED or REVIEWED tests can be published.");
		}
		List<TestQuestion> questions = questionRepository.findByTenantIdAndTestId(tenantId, testId);
		if (questions.isEmpty()) {
			throw new ReviewConflictException("Test " + testId + " has no questions and cannot be published.");
		}
		long needingReview = questions.stream().filter(TestQuestion::needsReview).count();
		if (needingReview > 0) {
			throw new ReviewConflictException("Test " + testId + " has " + needingReview
					+ " question(s) that need review and cannot be published.");
		}

		testRepository.updateStatus(tenantId, testId, TestStatus.PUBLISHED);
		history.statusChanged(tenantId, testId, TestStatus.PUBLISHED, actorUserId, comment,
				Map.of("previousStatus", current.name(), "questionCount", questions.size()));
		test.setStatus(TestStatus.PUBLISHED);
		return test;
	}

	private Test lockReviewableTest(Long tenantId, Long testId) {
		Test test = testRepository.findByTenantIdAndIdForUpdate(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
		if (test.getStatus() == null || !test.getStatus().isReviewable()) {
			throw new ReviewConflictException(
					"Test " + testId + " cannot be reviewed in status " + test.getStatus() + ".");
		}
		return test;
	}

	@Transactional
	public void recordQuestionEvent(TestQuestion question, TestEventType type, Long actorUserId, String comment,
			Map<String, Object> payload) {
		history.questionEvent(question, type, actorUserId, comment, payload);
	}

	/**
	 * REQUIRES_REVIEW while any question needs review. Once none does, a test
	 * that required review becomes REVIEWED. GENERATED and REVIEWED tests stay as
	 * they are when everything is OK.
	 */
	private void syncTestStatus(Long tenantId, Long testId, TestStatus current, Long actorUserId) {
		List<TestQuestion> questions = questionRepository.findByTenantIdAndTestId(tenantId, testId);
		long needingReview = questions.stream().filter(TestQuestion::needsReview).count();

		TestStatus target = needingReview > 0 ? TestStatus.REQUIRES_REVIEW
				: current == TestStatus.REQUIRES_REVIEW ? TestStatus.REVIEWED : current;
		if (target != current) {
			testRepository.updateStatus(tenantId, testId, target);
			history.statusChanged(tenantId, testId, target, actorUserId,
					Map.of("questionsNeedingReview", needingReview));
		}
	}
}
