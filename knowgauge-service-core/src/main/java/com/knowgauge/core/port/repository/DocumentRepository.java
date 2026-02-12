package com.knowgauge.core.port.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Document;

public interface DocumentRepository {
	Document save(Document document);

	Optional<Document> findById(Long id);

	Page<Document> findByTopicId(Long topicId, Pageable pageable);
	
	void updateStorageKey(Long id, String storageKey);
	
	int markIngesting(Long documentId);
	
	int markIngested(Long documentId);
	
	int markFailed(Long documentId, String errorMessage);
	
	void deleteById(Long id);
	
	boolean existsByTopicIdAndOriginalFileName(Long topicId, String originalFileName);

    boolean existsByTopicIdAndChecksum(Long topicId, String contentHash);
}