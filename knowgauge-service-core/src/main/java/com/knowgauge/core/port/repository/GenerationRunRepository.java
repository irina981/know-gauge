package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.GenerationRun;

public interface GenerationRunRepository {
	GenerationRun save(GenerationRun run);

	Optional<GenerationRun> findByTenantIdAndId(Long tenantId, Long id);

	/**
	 * @return runs of the test in chronological order
	 */
	List<GenerationRun> findByTenantIdAndTestId(Long tenantId, Long testId);
}
