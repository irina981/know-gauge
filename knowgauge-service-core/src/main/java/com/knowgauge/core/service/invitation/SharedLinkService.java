package com.knowgauge.core.service.invitation;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.AttemptConflictException;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.history.TestHistoryRecorder;
import com.knowgauge.core.util.PublicTokens;

import lombok.extern.slf4j.Slf4j;

/**
 * The shared link of a test (ADR009). A test has none until an administrator
 * creates it; regenerating replaces the token, so the old link stops working.
 * Attempts already started keep working: they are addressed by their own token.
 */
@Slf4j
@Service
public class SharedLinkService {

	private final TestRepository testRepository;
	private final TestHistoryRecorder history;
	private final ExecutionContext executionContext;

	public SharedLinkService(TestRepository testRepository, TestHistoryRecorder history,
			ExecutionContext executionContext) {
		this.testRepository = testRepository;
		this.history = history;
		this.executionContext = executionContext;
	}

	/**
	 * Creates the shared link, or replaces it with a new one.
	 *
	 * @return the test with its new {@code publicToken}
	 * @throws ResourceNotFoundException if the test does not exist
	 * @throws AttemptConflictException  if the test is not published
	 */
	@Transactional
	public Test createOrRegenerate(Long testId) {
		long tenantId = executionContext.tenantId();
		Test test = testRepository.findByTenantIdAndIdForUpdate(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
		if (test.getStatus() != TestStatus.PUBLISHED) {
			throw new AttemptConflictException("Test " + testId + " is not published (status " + test.getStatus()
					+ "). Publish it before sharing it.");
		}
		boolean regenerated = test.getPublicToken() != null;
		test.setPublicToken(PublicTokens.newToken());
		Test saved = testRepository.save(test);
		history.testEvent(tenantId, testId,
				regenerated ? TestEventType.SHARED_LINK_REGENERATED : TestEventType.SHARED_LINK_CREATED,
				executionContext.userId().isPresent() ? executionContext.userId().getAsLong() : null, Map.of());
		log.info("Test {} - shared link {}", testId, regenerated ? "regenerated, the old link no longer works" : "created");
		return saved;
	}
}
