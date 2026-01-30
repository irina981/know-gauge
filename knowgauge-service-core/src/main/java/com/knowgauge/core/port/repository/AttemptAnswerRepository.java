package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.AttemptAnswer;

public interface AttemptAnswerRepository {
	AttemptAnswer save(AttemptAnswer attemptAnswer);

	Optional<AttemptAnswer> findById(Long id);

	Page<AttemptAnswer> findByAttemptId(Long attemptId, Pageable pageable);
}