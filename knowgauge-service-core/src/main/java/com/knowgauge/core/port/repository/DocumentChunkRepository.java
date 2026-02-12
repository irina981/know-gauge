package com.knowgauge.core.port.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.DocumentChunk;

public interface DocumentChunkRepository {

	DocumentChunk save(DocumentChunk chunk);
	
	List<DocumentChunk> saveAll(List<DocumentChunk> chunks);

	Optional<DocumentChunk> findByTenantIdAndId(Long tenantId, Long id);

	Page<DocumentChunk> findByTenantIdAndDocumentIdAndDocumentVersion(Long tenantId, Long documentId,
			Integer documentVersion, Pageable pageable);

	List<DocumentChunk> findByTenantIdAndDocumentIdAndDocumentVersionOrderByOrdinal(Long tenantId, Long documentId,
			Integer documentVersion);

	List<DocumentChunk> findByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);

	void deleteByTenantIdAndDocumentIdAndDocumentVersion(Long tenantId, Long documentId, Integer documentVersion);

	Page<DocumentChunk> findByTenantIdAndTopicId(Long tenantId, Long topicId, Pageable pageable);

	public List<DocumentChunk> findByTenantIdAndTopicIdIn(Long tenantId, Collection<Long> topicIds);
	
	Optional<DocumentChunk> findByTenantIdAndChecksum(Long tenantId, String checksum);
}
