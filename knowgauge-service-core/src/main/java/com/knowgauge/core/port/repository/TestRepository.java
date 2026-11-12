package com.knowgauge.core.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;

public interface TestRepository {
	Test save(Test test);

	Optional<Test> findByTenantIdAndId(Long tenantId, Long id);

	/**
	 * Reads the test and locks its row until the caller's transaction ends.
	 * Serializes status changes (publish) with review actions on the same test.
	 * Must run inside a transaction.
	 */
	Optional<Test> findByTenantIdAndIdForUpdate(Long tenantId, Long id);

	List<Test> findByTenantId(Long tenantId);

	List<Test> findByTenantIdAndStatus(Long tenantId, TestStatus status);

	void deleteByTenantIdAndId(Long tenantId, Long id);

	void setUsedChunks(Long tenantId, Long testId, List<Long> chunkIds);

	int markGenerated(Long tenantId, Long testId, Instant finishedAt);

	int markFailed(Long tenantId, Long testId, String errorMessage, Instant failedAt);
	
	int markRequiresReview(Long tenantId, Long testId, Instant finishedAt);

	/**
	 * Sets the status without touching generation timestamps (review
	 * transitions).
	 */
	int updateStatus(Long tenantId, Long testId, TestStatus status);

	/**
	 * @return ids of the chunks retrieved for the test's generation
	 */
	List<Long> findUsedChunkIds(Long tenantId, Long testId);
}