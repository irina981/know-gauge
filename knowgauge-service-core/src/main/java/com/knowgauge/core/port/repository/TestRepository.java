package com.knowgauge.core.port.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;

public interface TestRepository {
	Test save(Test test);

	Optional<Test> findByTenantIdAndId(Long tenantId, Long id);

	List<Test> findByTenantId(Long tenantId);

	List<Test> findByTenantIdAndStatus(Long tenantId, TestStatus status);

	void deleteByTenantIdAndId(Long tenantId, Long id);

	void setUsedChunks(Long tenantId, Long testId, List<Long> chunkIds);

	int markGenerated(Long tenantId, Long testId, Instant finishedAt);

	int markFailed(Long tenantId, Long testId, String errorMessage, Instant failedAt);
}