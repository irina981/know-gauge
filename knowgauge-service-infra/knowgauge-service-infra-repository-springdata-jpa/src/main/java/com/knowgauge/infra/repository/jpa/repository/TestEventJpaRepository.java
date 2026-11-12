package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TestEventEntity;

@Repository
public interface TestEventJpaRepository extends JpaRepository<TestEventEntity, Long> {

	List<TestEventEntity> findByTenantIdAndTestIdOrderByCreatedAtAscIdAsc(Long tenantId, Long testId);

	List<TestEventEntity> findByTenantIdAndTestIdAndQuestionIdOrderByCreatedAtAscIdAsc(Long tenantId, Long testId,
			Long questionId);
}
