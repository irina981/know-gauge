package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.GenerationRun;

public interface GenerationRunRepository {
	GenerationRun save(GenerationRun run);

	Optional<GenerationRun> findById(Long id);

	Page<GenerationRun> findByTestId(Long testId, Pageable pageable);
}