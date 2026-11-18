package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.AttemptEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface AttemptJpaRepository extends JpaRepository<AttemptEntity, Long> {

	Optional<AttemptEntity> findByTenantIdAndId(Long tenantId, Long id);

	Optional<AttemptEntity> findByPublicToken(String publicToken);

	Optional<AttemptEntity> findByTenantIdAndInvitationId(Long tenantId, Long invitationId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			    select a
			      from AttemptEntity a
			     where a.tenantId = :tenantId
			       and a.id = :attemptId
			""")
	Optional<AttemptEntity> findByTenantIdAndIdForUpdate(@Param("tenantId") Long tenantId,
			@Param("attemptId") Long attemptId);

	@Query("""
			    select a
			      from AttemptEntity a
			     where a.tenantId = :tenantId
			       and (:testId is null or a.testId = :testId)
			       and (:userId is null or a.userId = :userId)
			       and (:participantEmail is null or a.participantEmail = :participantEmail)
			     order by a.startedAt desc, a.id desc
			""")
	List<AttemptEntity> findFiltered(@Param("tenantId") Long tenantId, @Param("testId") Long testId,
			@Param("userId") Long userId, @Param("participantEmail") String participantEmail);

	boolean existsByTenantIdAndTestId(Long tenantId, Long testId);
}
