package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.GenerationRunEntity;

@Repository
public interface GenerationRunJpaRepository extends JpaRepository<GenerationRunEntity, Long> {

	Optional<GenerationRunEntity> findByTenantIdAndId(Long tenantId, Long id);

	List<GenerationRunEntity> findByTenantIdAndTestIdOrderByIdAsc(Long tenantId, Long testId);
}
