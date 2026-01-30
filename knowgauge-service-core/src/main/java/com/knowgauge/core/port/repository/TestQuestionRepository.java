package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.TestQuestion;

public interface TestQuestionRepository {
	TestQuestion save(TestQuestion question);

	Optional<TestQuestion> findById(Long id);

	List<TestQuestion> findByTestId(Long testId);
}