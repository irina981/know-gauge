package com.knowgauge.core.port.repository;

import java.util.List;

import com.knowgauge.core.model.TestEvent;

/**
 * Append-only store of test history events (see ADR001).
 */
public interface TestEventRepository {

	TestEvent append(TestEvent event);

	List<TestEvent> appendAll(List<TestEvent> events);

	/**
	 * @return events of the test in chronological order
	 */
	List<TestEvent> findByTenantIdAndTestId(Long tenantId, Long testId);

	/**
	 * @return events of one question of the test in chronological order
	 */
	List<TestEvent> findByTenantIdAndTestIdAndQuestionId(Long tenantId, Long testId, Long questionId);
}
