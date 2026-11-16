package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.AttemptAnswer;

public interface AttemptAnswerRepository {
	AttemptAnswer save(AttemptAnswer attemptAnswer);

	/**
	 * @return the saved answers with ids set, in the given order
	 */
	List<AttemptAnswer> saveAll(List<AttemptAnswer> attemptAnswers);

	Optional<AttemptAnswer> findById(Long id);

	Page<AttemptAnswer> findByAttemptId(Long attemptId, Pageable pageable);

	List<AttemptAnswer> findByTenantIdAndAttemptId(Long tenantId, Long attemptId);
}
