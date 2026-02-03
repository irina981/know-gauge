package com.knowgauge.core.service.content;

import java.io.InputStream;

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
		document.setStatus(DocumentStatus.UPLOADED);
		document.setVersion(Integer.valueOf(1));
		document.setTenantId(executionContext.tenantId());
		Document savedDocument = documentRepository.save(document);
		
		// TODO: Once tenant and user logic is implemented, replace executionContext.tenantId() with savedDocument.getTenantId()
		String storageKey = storageKeyBuilder.build(executionContext.tenantId(), savedDocument.getId(), savedDocument.getVersion());

		StoredObject storedObj = storageService.put(storageKey, contentStream, savedDocument.getFileSizeBytes(),
				savedDocument.getContentType());

		documentRepository.updateStorageKey(savedDocument.getId(), storageKey);
		
		// TODO: think if we need eTag persisted for documents
		savedDocument.setEtag(storedObj.etag());

		return document;
	}
}
