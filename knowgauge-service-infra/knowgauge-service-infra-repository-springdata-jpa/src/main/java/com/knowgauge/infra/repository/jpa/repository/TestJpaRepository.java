package com.knowgauge.infra.repository.jpa.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.infra.repository.jpa.entity.TestEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface TestJpaRepository extends JpaRepository<TestEntity, Long> {
	Optional<TestEntity> findByTenantIdAndId(Long tenentId, Long testId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    select t
			      from TestEntity t
			     where t.tenantId = :tenantId
			       and t.id = :testId
			""")
	Optional<TestEntity> findByTenantIdAndIdForUpdate(@Param("tenantId") Long tenantId, @Param("testId") Long testId);

	List<TestEntity> findByTenantId(Long tenantId);

	List<TestEntity> findByTenantIdAndStatus(Long tenantId, TestStatus status);

	void deleteByTenantIdAndId(Long tenantId, Long testId);

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
			       set t.status = com.knowgauge.core.model.enums.TestStatus.REQUIRES_REVIEW,
			       t.generationFinishedAt = :finishedAt
			     where t.tenantId = :tenantId
			       and t.id = :testId
			""")
	int markRequiresReview(@Param("tenantId") Long tenantId, @Param("testId") Long testId,
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

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update TestEntity t
			       set t.status = :status
			     where t.tenantId = :tenantId
			       and t.id = :testId
			""")
	int updateStatus(@Param("tenantId") Long tenantId, @Param("testId") Long testId,
			@Param("status") TestStatus status);

	@Query(value = """
			select tuc.chunk_id
			  from test_used_chunks tuc
			  join tests t on t.id = tuc.test_id
			 where t.tenant_id = :tenantId
			   and t.id = :testId
			 order by tuc.chunk_id
			""", nativeQuery = true)
	List<Long> findUsedChunkIds(@Param("tenantId") Long tenantId, @Param("testId") Long testId);

}