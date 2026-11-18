package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.DocumentSection;

/** Sections of one tenant's documents (scoped through the document's {@code tenantId}). */
public interface DocumentSectionRepository {
	DocumentSection save(DocumentSection section);

	Optional<DocumentSection> findByTenantIdAndId(Long tenantId, Long id);

	Page<DocumentSection> findByTenantIdAndDocumentId(Long tenantId, Long documentId, Pageable pageable);
}
