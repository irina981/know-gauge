package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Attempt;

public interface AttemptRepository {
	Attempt save(Attempt attempt);

	Optional<Attempt> findById(Long id);

	Page<Attempt> findByTestId(Long testId, Pageable pageable);

	/**
	 * @return whether any attempt (in any status) exists for the test
	 */
	boolean existsByTenantIdAndTestId(Long tenantId, Long testId);
}