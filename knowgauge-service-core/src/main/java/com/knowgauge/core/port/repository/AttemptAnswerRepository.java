package com.knowgauge.core.port.repository;

import java.util.List;

import com.knowgauge.core.model.AttemptAnswer;

public interface AttemptAnswerRepository {
	AttemptAnswer save(AttemptAnswer attemptAnswer);

	/**
	 * @return the saved answers with ids set, in the given order
	 */
	List<AttemptAnswer> saveAll(List<AttemptAnswer> attemptAnswers);

	List<AttemptAnswer> findByTenantIdAndAttemptId(Long tenantId, Long attemptId);
}
