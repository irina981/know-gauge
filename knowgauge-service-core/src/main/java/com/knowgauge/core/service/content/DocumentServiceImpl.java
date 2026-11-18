package com.knowgauge.core.service.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.core.port.repository.TopicRepository;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.core.port.storage.StorageService.StoredObject;
import com.knowgauge.core.storage.StorageKeyBuilder;

/**
 * Documents of the own tenant and of the library (read-only unless the caller is an application admin, see
 * {@link ContentScope}). A document always lives in its topic's tenant.
 */
@Service
public class DocumentServiceImpl implements DocumentService {

	private final DocumentRepository documentRepository;
	private final TopicRepository topicRepository;
	private final StorageService storageService;
	private final StorageKeyBuilder storageKeyBuilder;
	private final ExecutionContext executionContext;
	private final ContentScope contentScope;

	public DocumentServiceImpl(DocumentRepository documentRepository, TopicRepository topicRepository,
			StorageService storageService, StorageKeyBuilder storageKeyBuilder, ExecutionContext executionContext,
			ContentScope contentScope) {
		this.documentRepository = documentRepository;
		this.topicRepository = topicRepository;
		this.storageService = storageService;
		this.storageKeyBuilder = storageKeyBuilder;
		this.executionContext = executionContext;
		this.contentScope = contentScope;
	}

	@Override
	@Transactional
	public Document uploadDocument(Document document, InputStream contentStream) {
		Long topicId = document.getTopicId();
		Topic topic = topicId == null ? null
				: topicRepository.findByTenantIdInAndId(contentScope.readableTenantIds(), topicId).orElse(null);
		if (topic == null) {
			throw new ResourceNotFoundException("Topic not found: " + topicId);
		}
		Long tenantId = topic.getTenantId();
		contentScope.requireWritable(tenantId, "Topic " + topicId);
		validateUniqueNameUnderTopic(tenantId, topicId, document.getOriginalFileName());
		validateUniqueContentUnderTopic(tenantId, topicId, document.getChecksum());

		document.setStatus(DocumentStatus.UPLOADED);
		document.setVersion(Integer.valueOf(1));
		document.setTenantId(tenantId);
		Document savedDocument = documentRepository.save(document);

		String storageKey = storageKeyBuilder.build(tenantId, savedDocument.getId(), savedDocument.getVersion());

		StoredObject storedObj = storageService.put(storageKey, contentStream, savedDocument.getFileSizeBytes(),
				savedDocument.getContentType());

		documentRepository.updateStorageKey(tenantId, savedDocument.getId(), storageKey);
		savedDocument.setStorageKey(storageKey);

		// TODO: think if we need eTag persisted for documents
		savedDocument.setEtag(storedObj.etag());

		return savedDocument;
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Document document = get(id).orElse(null);
		if (null != document) {
			contentScope.requireWritable(document.getTenantId(), "Document " + id);
			documentRepository.deleteByTenantIdAndId(document.getTenantId(), id);
			storageService.delete(document.getStorageKey());
		}
	}

	@Override
	@Transactional
	public Page<Document> getAllDocuments(Long topicId, Pageable pageable) {
		return documentRepository.findByTenantIdInAndTopicId(contentScope.readableTenantIds(), topicId, pageable);
	}

	@Override
	@Transactional
	public Optional<Document> get(Long id) {
		return documentRepository.findByTenantIdInAndId(contentScope.readableTenantIds(), id);
	}

	@Override
	@Transactional
	public Document download(Long id, OutputStream out) {
		Document document = require(id);
		storageService.download(document.getStorageKey(), out);
		return document;
	}

	@Override
	@Transactional
	public InputStream download(Long id) {
		return storageService.download(require(id).getStorageKey());
	}

	private Document require(Long id) {
		return get(id).orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
	}

	private void validateUniqueNameUnderTopic(Long tenantId, Long topicId, String originalFileName) {
		if (documentRepository.existsByTenantIdAndTopicIdAndOriginalFileName(tenantId, topicId, originalFileName)) {
			throw new IllegalArgumentException("Document with name \"" + originalFileName + "\" already exists for topic id " + topicId);
		}
	}

	private void validateUniqueContentUnderTopic(Long tenantId, Long topicId, String checksum) {
		if (documentRepository.existsByTenantIdAndTopicIdAndChecksum(tenantId, topicId, checksum)) {
			throw new IllegalArgumentException("Document with the same content already exists for topic id " + topicId);
		}
	}

	// Ingestion runs in the document's own tenant (IngestionServiceImpl switches the context to it).

	@Override
	public int markIngesting(Long documentId) {
		return documentRepository.markIngesting(executionContext.tenantId(), documentId);
	}

	@Override
	public int markIngested(Long documentId) {
		return documentRepository.markIngested(executionContext.tenantId(), documentId);
	}

	@Override
	public int markFailed(Long documentId, String errorMessage) {
		return documentRepository.markFailed(executionContext.tenantId(), documentId, errorMessage);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Long, Long> countIngestedByTopic() {
		return documentRepository.countByTopicId(contentScope.readableTenantIds(), DocumentStatus.INGESTED);
	}

	@Override
	public List<Document> findByTopicIds(Collection<Long> tenantIds, List<Long> topicIds) {
		return documentRepository.findByTenantIdInAndTopicIdIn(tenantIds, topicIds);
	}

	@Override
	public List<Document> findOldestUploaded(Long tenantId, int limit) {
		return documentRepository.findOldestByTenantIdAndStatus(tenantId, DocumentStatus.UPLOADED, limit);
	}

	@Override
	public List<Long> findTenantIdsWithUploaded() {
		return documentRepository.findTenantIdsByStatus(DocumentStatus.UPLOADED);
	}
}
