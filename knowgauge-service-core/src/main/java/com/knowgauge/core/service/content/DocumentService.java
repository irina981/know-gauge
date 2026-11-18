package com.knowgauge.core.service.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Document;

public interface DocumentService {

	/**
	 * Uploads a document and stores metadata + objectKey. Ingestion/chunking can be
	 * triggered later (separate endpoint/job).
	 */
	Document uploadDocument(Document document, InputStream contentStream);

	Optional<Document> get(Long id);

	Document download(Long id, OutputStream out);

	InputStream download(Long id);

	void delete(Long topicId);

	Page<Document> getAllDocuments(Long topicId, Pageable pageable);

	int markIngesting(Long documentId);

	int markIngested(Long documentId);

	int markFailed(Long documentId, String errorMessage);
	
	List<Document> findByTopicIds(Collection<Long> tenantIds, List<Long> topicIds);

	/**
	 * Ingested documents per topic of the current tenant, counting only documents
	 * directly in each topic (not in sub-topics). Topics without any are left out.
	 */
	Map<Long, Long> countIngestedByTopic();

	/**
	 * Returns up to {@code limit} {@code UPLOADED} documents of the tenant, oldest first.
	 */
	List<Document> findOldestUploaded(Long tenantId, int limit);

	/**
	 * Returns the ids of all tenants that have at least one {@code UPLOADED}
	 * document, in ascending order. Used by jobs that run for every tenant.
	 */
	List<Long> findTenantIdsWithUploaded();
}
