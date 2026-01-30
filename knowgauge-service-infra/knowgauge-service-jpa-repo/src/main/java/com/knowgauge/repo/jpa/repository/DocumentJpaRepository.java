package com.knowgauge.repo.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.DocumentEntity;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {

	Page<DocumentEntity> findByTopicId(Long topicId, Pageable pageable);
}