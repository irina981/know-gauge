package com.knowgauge.repo.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.GenerationRunEntity;

@Repository
public interface GenerationRunJpaRepository extends JpaRepository<GenerationRunEntity, Long> {

	Page<GenerationRunEntity> findByTestId(Long testId, Pageable pageable);
}