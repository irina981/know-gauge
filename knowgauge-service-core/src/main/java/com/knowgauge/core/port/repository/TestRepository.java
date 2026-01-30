package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Test;

public interface TestRepository {
	Test save(Test test);

	Optional<Test> findById(Long id);

	Page<Test> findByTopicId(Long topicId, Pageable pageable);
}