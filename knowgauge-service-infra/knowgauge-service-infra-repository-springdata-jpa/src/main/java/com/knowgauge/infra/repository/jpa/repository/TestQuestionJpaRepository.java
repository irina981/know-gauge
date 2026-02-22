package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TestQuestionEntity;

@Repository
public interface TestQuestionJpaRepository extends JpaRepository<TestQuestionEntity, Long> {

	List<TestQuestionEntity> findByTestId(Long testId);
}