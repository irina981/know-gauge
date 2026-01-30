package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.DocumentSection;

public interface DocumentSectionRepository {
	DocumentSection save(DocumentSection section);

	Optional<DocumentSection> findById(Long id);

	Page<DocumentSection> findByDocumentId(Long documentId, Pageable pageable);
}