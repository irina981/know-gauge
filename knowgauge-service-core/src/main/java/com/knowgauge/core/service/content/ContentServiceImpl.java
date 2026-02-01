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
public class ContentServiceImpl implements ContentService {

	private final DocumentRepository documentRepository;
	private final TopicRepository topicRepository;
	private final StorageService storageService;
	private final StorageProperties storageProperties;

	public ContentServiceImpl(DocumentRepository documentRepository, TopicRepository topicRepository,
			StorageService storageService, StorageProperties storageProperties) {
		this.documentRepository = documentRepository;
		this.topicRepository = topicRepository;
		this.storageService = storageService;
		this.storageProperties = storageProperties;
	}

	@Override
	@Transactional
	public Topic createTopic(Topic topic) {
		Long parentId = topic.getParentId();
		if (null != parentId && topicRepository.existsByParentIdAndName(parentId, topic.getName())) {
			throw new IllegalArgumentException("Topic with name " + topic.getName() + "already exists for parent id " + parentId);
		}
		return topicRepository.save(topic);
	}

	@Override
	@Transactional
	public Topic createTopicTree(Topic req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Transactional
	public Document uploadDocument(Document document, InputStream contentStream) {
		String objectKey = storageProperties.getObjectKeyTemplate().formatted(document.getId(), document.getOriginalFileName());
		document.setStorageKey(objectKey);
		document = documentRepository.save(document);
		
		StoredObject storedObj = storageService.put(objectKey, contentStream, document.getFileSizeBytes(), document.getContentType());
		
		document.setEtag(storedObj.etag());
		
		return document;
	}
}
