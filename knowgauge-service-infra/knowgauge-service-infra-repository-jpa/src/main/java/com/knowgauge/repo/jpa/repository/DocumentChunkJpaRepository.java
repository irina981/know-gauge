package com.knowgauge.repo.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.DocumentChunkEntity;

@Repository
public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkEntity, Long> {
	
	Optional<DocumentChunkEntity> findByTenantIdAndId(Long tenantId, Long id);

	// -------------------------------------------------
	// Core access pattern: chunks of a document version
	// -------------------------------------------------

	List<DocumentChunkEntity> findByTenantIdAndDocumentIdAndDocumentVersionOrderByOrdinal(Long tenantId,
			Long documentId, Integer documentVersion);

	Page<DocumentChunkEntity> findByTenantIdAndDocumentIdAndDocumentVersion(Long tenantId, Long documentId,
			Integer documentVersion, Pageable pageable);

	// -------------------------------------------------
	// Topic-based filtering (used for retrieval scope)
	// -------------------------------------------------

	Page<DocumentChunkEntity> findByTenantIdAndTopicId(Long tenantId, Long topicId, Pageable pageable);

	List<DocumentChunkEntity> findByTenantIdAndTopicIdIn(Long tenantId, Collection<Long> topicIds);

	// -------------------------------------------------
	// Fetch chunks by IDs (after vector search)
	// -------------------------------------------------

	List<DocumentChunkEntity> findByTenantIdAndIdIn(Long tenantId, Collection<Long> chunkIds);

	// -------------------------------------------------
	// Optional: checksum-based lookup (debug / dedupe)
	// -------------------------------------------------

	Optional<DocumentChunkEntity> findByTenantIdAndChecksum(Long tenantId, String checksum);

	// -------------------------------------------------
	// Optional: delete on re-ingestion
	// -------------------------------------------------

	void deleteByTenantIdAndDocumentIdAndDocumentVersion(Long tenantId, Long documentId, Integer documentVersion);
}
