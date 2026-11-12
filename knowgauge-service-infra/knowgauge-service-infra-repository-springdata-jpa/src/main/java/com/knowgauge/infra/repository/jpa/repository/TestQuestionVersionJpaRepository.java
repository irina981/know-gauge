package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TestQuestionVersionEntity;

@Repository
public interface TestQuestionVersionJpaRepository extends JpaRepository<TestQuestionVersionEntity, Long> {

	List<TestQuestionVersionEntity> findByTenantIdAndQuestionIdOrderByVersionNoAsc(Long tenantId, Long questionId);

	List<TestQuestionVersionEntity> findByTenantIdAndIdIn(Long tenantId, List<Long> ids);

	@Query("select coalesce(max(v.versionNo), 0) from TestQuestionVersionEntity v where v.tenantId = :tenantId and v.questionId = :questionId")
	int findMaxVersionNo(@Param("tenantId") Long tenantId, @Param("questionId") Long questionId);
}
