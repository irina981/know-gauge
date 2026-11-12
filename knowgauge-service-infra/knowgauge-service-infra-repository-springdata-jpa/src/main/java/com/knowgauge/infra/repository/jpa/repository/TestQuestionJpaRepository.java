package com.knowgauge.infra.repository.jpa.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.enums.QuestionStatus;
import com.knowgauge.infra.repository.jpa.entity.TestQuestionEntity;

@Repository
public interface TestQuestionJpaRepository extends JpaRepository<TestQuestionEntity, Long> {

	Optional<TestQuestionEntity> findByTenantIdAndId(Long tenantId, Long id);

	List<TestQuestionEntity> findByTenantIdAndTestIdOrderByQuestionIndexAsc(Long tenantId, Long testId);

	/**
	 * Optimistic switch: only succeeds while the slot still points at the
	 * expected version.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update TestQuestionEntity q
			   set q.currentVersionId = :newVersionId,
			       q.status = :status,
			       q.updatedAt = :now,
			       q.updatedBy = :actor
			 where q.tenantId = :tenantId
			   and q.id = :questionId
			   and q.currentVersionId = :expectedVersionId
			""")
	int switchCurrentVersion(@Param("tenantId") Long tenantId, @Param("questionId") Long questionId,
			@Param("expectedVersionId") Long expectedVersionId, @Param("newVersionId") Long newVersionId,
			@Param("status") QuestionStatus status, @Param("actor") Long actor, @Param("now") Instant now);
}
