package com.knowgauge.core.service.content;

import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.core.port.repository.TopicRepository;
import com.knowgauge.core.port.storage.StorageService;
import com.knowgauge.core.port.storage.StorageService.StoredObject;
import com.knowgauge.core.properties.StorageProperties;

@Service
public class DocumentServiceImpl implements DocumentService {

	private final DocumentRepository documentRepository;
	private final StorageService storageService;
	private final StorageProperties storageProperties;

	public DocumentServiceImpl(DocumentRepository documentRepository, StorageService storageService,
			StorageProperties storageProperties) {
		this.documentRepository = documentRepository;
		this.storageService = storageService;
		this.storageProperties = storageProperties;
	}

	@Override
	@Transactional
	public Document uploadDocument(Document document, InputStream contentStream) {
		String objectKey = storageProperties.getObjectKeyTemplate().formatted(document.getId(),
				document.getOriginalFileName());
		document.setStorageKey(objectKey);
		document = documentRepository.save(document);

		StoredObject storedObj = storageService.put(objectKey, contentStream, document.getFileSizeBytes(),
				document.getContentType());

		document.setEtag(storedObj.etag());

		return document;
	}
}
