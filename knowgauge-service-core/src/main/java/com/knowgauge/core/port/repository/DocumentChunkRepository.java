package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.DocumentChunk;

public interface DocumentChunkRepository {
	DocumentChunk save(DocumentChunk chunk);

	Optional<DocumentChunk> findById(Long id);

	Page<DocumentChunk> findByDocumentId(Long documentId, Pageable pageable);
}