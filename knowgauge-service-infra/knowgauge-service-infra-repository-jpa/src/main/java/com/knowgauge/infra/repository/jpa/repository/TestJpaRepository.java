package com.knowgauge.infra.repository.jpa.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TestEntity;

@Repository
public interface TestJpaRepository extends JpaRepository<TestEntity, Long> {
	Optional<TestEntity> findByTenantIdAndId(Long tenentId, Long testId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update TestEntity t
			       set t.status = com.knowgauge.core.model.enums.TestStatus.GENERATED,
			           t.generationFinishedAt = :finishedAt
			     where t.tenantId = :tenantId
			       and t.id = :testId
			""")
	int markGenerated(@Param("tenantId") Long tenantId, @Param("testId") Long testId,
			@Param("finishedAt") Instant finishedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update TestEntity t
			       set t.status = com.knowgauge.core.model.enums.TestStatus.FAILED,
			           t.generationErrorMessage = :errorMessage,
			           t.generationFailedAt = :failedAt
			     where t.tenantId = :tenantId
			       and t.id = :testId
			""")
	int markFailed(@Param("tenantId") Long tenantId, @Param("testId") Long testId,
			@Param("errorMessage") String errorMessage, @Param("failedAt") Instant failedAt);

}