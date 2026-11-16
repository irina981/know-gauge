package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Attempt;

public interface AttemptRepository {
	Attempt save(Attempt attempt);

	Optional<Attempt> findById(Long id);

	Optional<Attempt> findByTenantIdAndId(Long tenantId, Long id);

	/**
	 * Reads the attempt and locks its row until the caller's transaction ends, so
	 * the same attempt cannot be submitted twice concurrently. Must run inside a
	 * transaction.
	 */
	Optional<Attempt> findByTenantIdAndIdForUpdate(Long tenantId, Long id);

	/**
	 * Attempts of the tenant, newest first. {@code testId} and {@code userId} are
	 * optional filters ({@code null} = any).
	 */
	List<Attempt> findByTenantId(Long tenantId, Long testId, Long userId);

	Page<Attempt> findByTestId(Long testId, Pageable pageable);

	/**
	 * @return whether any attempt (in any status) exists for the test
	 */
	boolean existsByTenantIdAndTestId(Long tenantId, Long testId);
}
