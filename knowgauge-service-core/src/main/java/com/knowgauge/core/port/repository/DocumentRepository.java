package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.enums.DocumentStatus;

public interface DocumentRepository {
	Document save(Document document);

	Optional<Document> findById(Long id);

	Page<Document> findByTopicId(Long topicId, Pageable pageable);
	
	List<Document> findByTenantIdAndTopicIdIn(Long tenantId, List<Long> topicId);

	/**
	 * Returns up to {@code limit} documents of the tenant in the given status, oldest (by creation time) first.
	 */
	List<Document> findOldestByTenantIdAndStatus(Long tenantId, DocumentStatus status, int limit);
	
	void updateStorageKey(Long id, String storageKey);
	
	int markIngesting(Long documentId);
	
	int markIngested(Long documentId);
	
	int markFailed(Long documentId, String errorMessage);
	
	void deleteById(Long id);
	
	boolean existsByTopicIdAndOriginalFileName(Long topicId, String originalFileName);

    boolean existsByTopicIdAndChecksum(Long topicId, String contentHash);
}