package com.knowgauge.repo.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.AttemptAnswerEntity;

@Repository
public interface AttemptAnswerJpaRepository extends JpaRepository<AttemptAnswerEntity, Long> {
	
	Page<AttemptAnswerEntity> findByAttemptId(Long attemptId, Pageable pageable);
}