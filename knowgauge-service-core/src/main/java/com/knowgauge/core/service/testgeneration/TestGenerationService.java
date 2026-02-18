package com.knowgauge.core.service.testgeneration;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;

public interface TestGenerationService {
	public Test generate(Test test);

	Optional<Test> getById(Long testId);

	List<Test> getAll();

	List<Test> getAllByStatus(TestStatus status);

	void deleteById(Long testId);

}
