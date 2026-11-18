package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.AttemptAnswerEntity;

@Repository
public interface AttemptAnswerJpaRepository extends JpaRepository<AttemptAnswerEntity, Long> {

	List<AttemptAnswerEntity> findByTenantIdAndAttemptId(Long tenantId, Long attemptId);
}
