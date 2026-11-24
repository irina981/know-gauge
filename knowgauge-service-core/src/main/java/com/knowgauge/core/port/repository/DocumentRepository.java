package com.knowgauge.core.port.repository;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.enums.DocumentStatus;

/**
 * Documents, always filtered by tenant. Reads take the tenants whose content may be read (own tenant and library,
 * see {@code ContentScope}); writes take the one tenant written to. The exception is
 * {@link #findTenantIdsByStatus}, which the scheduled ingestion job uses to find the tenants to work for.
 */
public interface DocumentRepository {
	Document save(Document document);

	Optional<Document> findByTenantIdInAndId(Collection<Long> tenantIds, Long id);

	Page<Document> findByTenantIdInAndTopicId(Collection<Long> tenantIds, Long topicId, Pageable pageable);

	List<Document> findByTenantIdInAndTopicIdIn(Collection<Long> tenantIds, List<Long> topicIds);

	/**
	 * Returns up to {@code limit} documents of the tenant in the given status, oldest (by creation time) first.
	 */
	List<Document> findOldestByTenantIdAndStatus(Long tenantId, DocumentStatus status, int limit);

	/** @return ids of the tenants that have documents in {@code status}, ascending */
	List<Long> findTenantIdsByStatus(DocumentStatus status);

	void updateStorageKey(Long tenantId, Long id, String storageKey);

	int markIngesting(Long tenantId, Long documentId);

	int markIngested(Long tenantId, Long documentId);

	int markFailed(Long tenantId, Long documentId, String errorMessage);

	void deleteByTenantIdAndId(Long tenantId, Long id);

	/** @return the number of documents with the given status per topic id (topics without any are left out) */
	Map<Long, Long> countByTopicId(Collection<Long> tenantIds, DocumentStatus status);

	boolean existsByTenantIdAndTopicIdAndOriginalFileName(Long tenantId, Long topicId, String originalFileName);

	boolean existsByTenantIdAndTopicIdAndChecksum(Long tenantId, Long topicId, String contentHash);

	/** @return the total size of the tenant's documents in bytes */
	long sumFileSizeBytesByTenantId(Long tenantId);
}
