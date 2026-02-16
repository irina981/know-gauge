package com.knowgauge.core.port.repository;

import java.util.Optional;

import com.knowgauge.core.model.Test;

public interface TestRepository {
	Test save(Test test);

	Optional<Test> findByTenantIdAndId(Long tenantId, Long id);
}