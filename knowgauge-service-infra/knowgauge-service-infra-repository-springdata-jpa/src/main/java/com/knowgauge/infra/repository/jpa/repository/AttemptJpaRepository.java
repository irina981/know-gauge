package com.knowgauge.infra.repository.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.AttemptEntity;

@Repository
public interface AttemptJpaRepository extends JpaRepository<AttemptEntity, Long> {

	Page<AttemptEntity> findByTestId(Long testId, Pageable pageable);
}