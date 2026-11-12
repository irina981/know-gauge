package com.knowgauge.core.service.testgeneration;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.history.TestHistoryRecorder;

/**
 * Short transactions of the generation flow. Every state change is recorded in
 * the test history in the same transaction.
 */
@Service
public class TestGenerationTransactionalServiceImpl {

	private final TestRepository testRepository;
	private final TestQuestionRepository testQuestionRepository;
	private final TestHistoryRecorder history;

	public TestGenerationTransactionalServiceImpl(TestRepository testRepository,
			TestQuestionRepository testQuestionRepository, TestHistoryRecorder history) {
		this.testRepository = testRepository;
		this.testQuestionRepository = testQuestionRepository;
		this.history = history;
	}

	@Transactional
	public Test persistTest(Long tenantId, Test toSave, Long actorUserId) {
		toSave.setTenantId(tenantId);
		toSave.setStatus(TestStatus.CREATED);
		toSave.setGenerationStartedAt(Instant.now());

		Map<String, Object> params = toSave.getGenerationParams();
		if (params == null) {
			params = new HashMap<>();
		}
		params.remove("error"); // Just in case, remove any error info from previous failed attempt.
		toSave.setGenerationParams(params);

		Test saved = testRepository.save(toSave);
		history.testEvent(tenantId, saved.getId(), TestEventType.TEST_CREATED, actorUserId, null);
		return saved;
	}

	/**
	 * Persists generated drafts as question slots with version 1, and records
	 * QUESTION_GENERATED / OPTION_FLAGGED events.
	 *
	 * @param defaultSourceChunkIds sources for questions that cited none
	 */
	@Transactional
	public List<TestQuestion> persistGeneratedQuestions(Long tenantId, Long testId, List<TestQuestion> questions,
			List<Long> defaultSourceChunkIds) {
		for (TestQuestion q : questions) {
			q.setTenantId(tenantId);
			q.setTestId(testId);
			q.setStatus(q.deriveStatus());

			TestQuestionVersion v = q.getCurrentVersion();
			v.setTenantId(tenantId);
			v.setVersionNo(1);
			v.setParentVersionId(null);
			v.setCreatedBy(null); // system
			v.setCreatedAt(Instant.now());
			if (v.getSourceChunkIds() == null || v.getSourceChunkIds().isEmpty()) {
				v.setSourceChunkIds(defaultSourceChunkIds);
			}
		}
		List<TestQuestion> saved = testQuestionRepository.saveNewWithInitialVersions(questions);
		history.questionsGenerated(saved);
		return saved;
	}

	@Transactional
	public Test markTestGenerated(Long tenantId, Long testId) {
		int updated = testRepository.markGenerated(tenantId, testId, Instant.now());
		if (updated == 0) {
			throw new IllegalArgumentException("Test not found: " + testId);
		}
		history.statusChanged(tenantId, testId, TestStatus.GENERATED, null, null);
		return testRepository.findByTenantIdAndId(tenantId, testId).orElseThrow();
	}

	@Transactional
	public Test markTestRequiresReview(Long tenantId, Long testId, int questionsNeedingReview) {
		int updated = testRepository.markRequiresReview(tenantId, testId, Instant.now());
		if (updated == 0) {
			throw new IllegalArgumentException("Test not found: " + testId);
		}
		history.statusChanged(tenantId, testId, TestStatus.REQUIRES_REVIEW, null,
				Map.of("questionsNeedingReview", questionsNeedingReview));
		return testRepository.findByTenantIdAndId(tenantId, testId).orElseThrow();
	}

	@Transactional
	public void markTestFailed(Long tenantId, Long testId, String errorMessage) {
		int updated = testRepository.markFailed(tenantId, testId, errorMessage, Instant.now());
		if (updated == 0) {
			throw new IllegalArgumentException("Test not found: " + testId);
		}
		Map<String, Object> payload = new HashMap<>();
		payload.put("error", errorMessage);
		history.statusChanged(tenantId, testId, TestStatus.FAILED, null, payload);
	}

	@Transactional
	public void setUsedChunks(Long tenantId, Long testId, List<Long> chunkIds) {
		testRepository.setUsedChunks(tenantId, testId, chunkIds);
	}
}
