package com.knowgauge.repo.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.DocumentSectionEntity;

@Repository
public interface DocumentSectionJpaRepository extends JpaRepository<DocumentSectionEntity, Long> {

	Page<DocumentSectionEntity> findByDocumentId(Long documentId, Pageable pageable);
}