package com.knowgauge.core.service.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.core.port.storage.StorageService.StoredObject;
import com.knowgauge.core.storage.StorageKeyBuilder;

@Service
public class DocumentServiceImpl implements DocumentService {

	private final DocumentRepository documentRepository;
	private final StorageService storageService;
	private final StorageKeyBuilder storageKeyBuilder;
	private final ExecutionContext executionContext;

	public DocumentServiceImpl(DocumentRepository documentRepository, StorageService storageService,
			StorageKeyBuilder storageKeyBuilder, ExecutionContext executionContext) {
		this.documentRepository = documentRepository;
		this.storageService = storageService;
		this.storageKeyBuilder = storageKeyBuilder;
		this.executionContext = executionContext;
	}

	@Override
	@Transactional
	public Document uploadDocument(Document document, InputStream contentStream) {
		validateUniqueNameUnderTopic(document.getTopicId(), document.getOriginalFileName());
		validateUniqueContentUnderTopic(document.getTopicId(), document.getChecksum());
		
		document.setStatus(DocumentStatus.UPLOADED);
		document.setVersion(Integer.valueOf(1));
		document.setTenantId(executionContext.tenantId());
		Document savedDocument = documentRepository.save(document);

		// TODO: Once tenant and user logic is implemented, replace
		// executionContext.tenantId() with savedDocument.getTenantId()
		String storageKey = storageKeyBuilder.build(executionContext.tenantId(), savedDocument.getId(),
				savedDocument.getVersion());

		StoredObject storedObj = storageService.put(storageKey, contentStream, savedDocument.getFileSizeBytes(),
				savedDocument.getContentType());

		documentRepository.updateStorageKey(savedDocument.getId(), storageKey);
		savedDocument.setStorageKey(storageKey);

		// TODO: think if we need eTag persisted for documents
		savedDocument.setEtag(storedObj.etag());

		return savedDocument;
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Document document = documentRepository.findById(id).orElse(null);
		if (null != document) {
			documentRepository.deleteById(id);
			storageService.delete(document.getStorageKey());
		}	
	}

	@Override
	@Transactional
	public Page<Document> getAllDocuments(Long topicId, Pageable pageable) {
		return documentRepository.findByTopicId(topicId, pageable);
	}

	@Override
	@Transactional
	public Optional<Document> get(Long id) {
		return documentRepository.findById(id);
	}

	@Override
	@Transactional
	public Document download(Long id, OutputStream out) {
		Document document = documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
		storageService.download(document.getStorageKey(), out);
		return document;
	}
	
	@Override
	@Transactional
	public InputStream download(Long id) {
		Document document = documentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
		return storageService.download(document.getStorageKey());
	}
	
	private void validateUniqueNameUnderTopic(Long topicId, String originalFileName) {
		if (documentRepository.existsByTopicIdAndOriginalFileName(topicId, originalFileName)) {
			throw new IllegalArgumentException("Document with name \"" + originalFileName + "\" already exists for topic id " + topicId);
		}
	}
	
	private void validateUniqueContentUnderTopic(Long topicId, String checksum) {
		if (documentRepository.existsByTopicIdAndChecksum(topicId, checksum)) {
			throw new IllegalArgumentException("Document with the same content already exists for topic id " + topicId);
		}
	}

	@Override
	public int updateStatusIfCurrent(Long documentId, DocumentStatus fromStatus,
			DocumentStatus toStatus) {
		return documentRepository.updateStatusIfCurrent(documentId, fromStatus, toStatus);
	}

	@Override
	public int markIngested(Long documentId) {
		return documentRepository.markIngested(documentId);
	}

	@Override
	public int markFailed(Long documentId, String errorMessage) {
		return documentRepository.markFailed(documentId, errorMessage);
	}
	
	
}
