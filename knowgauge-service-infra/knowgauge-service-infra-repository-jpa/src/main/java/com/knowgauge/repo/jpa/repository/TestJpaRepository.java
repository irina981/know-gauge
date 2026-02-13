package com.knowgauge.repo.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.TestEntity;

@Repository
public interface TestJpaRepository extends JpaRepository<TestEntity, Long> {

	Page<TestEntity> findByTopicId(Long topicId, Pageable pageable);
}