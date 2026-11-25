package com.knowgauge.restapi.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.contract.api.ContentApi;
import com.knowgauge.contract.dto.DocumentDto;
import com.knowgauge.contract.dto.DocumentInput;
import com.knowgauge.contract.dto.TopicCreateInput;
import com.knowgauge.contract.dto.TopicDocumentCountDto;
import com.knowgauge.contract.dto.TopicDto;
import com.knowgauge.contract.dto.TopicTreeNodeInput;
import com.knowgauge.contract.dto.TopicUpdateInput;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;
import com.knowgauge.core.service.ingestion.IngestionService;
import com.knowgauge.core.util.HashingHelper;
import com.knowgauge.restapi.mapper.DocumentMapper;
import com.knowgauge.restapi.mapper.TopicMapper;
import com.knowgauge.restapi.security.Roles;
import com.knowgauge.restapi.util.TempFilesHelper;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class ContentController implements ContentApi {

	private final DocumentService documentService;
	private final TopicService topicService;
	private final TopicMapper topicMapper;
	private final DocumentMapper documentMaper;
	private final ObjectMapper objectMapper;
	private final IngestionService ingestionService;

	public ContentController(DocumentService documentService, TopicService topicService, TopicMapper topicMapper,
			DocumentMapper documentMaper, ObjectMapper objectMapper, IngestionService ingestionService) {
		this.documentService = documentService;
		this.topicService = topicService;
		this.documentMaper = documentMaper;
		this.topicMapper = topicMapper;
		this.objectMapper = objectMapper;
		this.ingestionService = ingestionService;
	}

	// -----------------------------
	// Topics
	// -----------------------------

	@Override
	public ResponseEntity<TopicDto> getTopic(Long id) {
		Topic topic = topicService.get(id).orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
		return ResponseEntity.ok(topicMapper.toDto(topic));
	}

	@Override
	public ResponseEntity<List<TopicDto>> getRoots() {
		List<TopicDto> topics = topicService.getRoots().stream().map(topicMapper::toDto).toList();
		return ResponseEntity.status(200).body(topics);
	}

	@Override
	public ResponseEntity<List<TopicDto>> getChildren(Long parentId) {
		List<TopicDto> topics = topicService.getChildren(parentId).stream().map(topicMapper::toDto).toList();
		return ResponseEntity.status(200).body(topics);
	}

	@Override
	public ResponseEntity<List<TopicDocumentCountDto>> getIngestedDocumentCounts() {
		return ResponseEntity.ok(documentService.countIngestedByTopic().entrySet().stream()
				.map(e -> new TopicDocumentCountDto(e.getKey(), e.getValue())).toList());
	}

	@Override
	public ResponseEntity<List<TopicDto>> getDescendants(Long parentId) {
		List<TopicDto> topics = topicService.getDescendants(parentId).stream().map(topicMapper::toDto).toList();
		return ResponseEntity.status(200).body(topics);
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<Void> deleteTopic(Long id) {
		topicService.delete(id);
		return ResponseEntity.status(200).body(null);
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<TopicDto> createTopic(TopicCreateInput topicInput) {
		Topic created = topicService.create(topicMapper.toDomain(topicInput));

		// If Topic has an ID, you can return Location header.
		// Otherwise, OK to just return 201 with body.
		URI location = tryBuildLocation("/api/content/topics/{id}", created);
		return (location != null) ? ResponseEntity.created(location).body(topicMapper.toDto(created))
				: ResponseEntity.status(201).body(topicMapper.toDto(created));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<TopicDto> updateTopic(Long id, TopicUpdateInput topicInput) {
		Topic topic = topicMapper.toDomain(topicInput);
		topic.setId(id);
		Topic updated = topicService.update(topic);
		return ResponseEntity.ok(topicMapper.toDto(updated));
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<TopicDto> createTopicTree(TopicTreeNodeInput topicTreeNodeInput) {
		Topic createdRoot = topicService.createTopicTree(topicMapper.toDomain(topicTreeNodeInput));

		URI location = tryBuildLocation("/api/content/topics/{id}", createdRoot);
		return (location != null) ? ResponseEntity.created(location).body(topicMapper.toDto(createdRoot))
				: ResponseEntity.status(201).body(topicMapper.toDto(createdRoot));
	}

	// -----------------------------
	// Documents
	// -----------------------------

	@Override
	public ResponseEntity<DocumentDto> getDocument(Long id) {
		Document document = documentService.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
		return ResponseEntity.ok(documentMaper.toDto(document));
	}

	@Override
	public ResponseEntity<Page<DocumentDto>> getAllDocuments(Long topicId, Pageable pageable) {
		Page<DocumentDto> documents = documentService.getAllDocuments(topicId, pageable).map(documentMaper::toDto);
		return ResponseEntity.status(200).body(documents);
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<DocumentDto> uploadDocument(String metaJson, MultipartFile file) throws IOException {

		// Basic defensive checks (optional but recommended)
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		// Create document domain object
		DocumentInput documentInput = objectMapper.readValue(metaJson, DocumentInput.class);
		Document document = documentMaper.toDomain(documentInput, file);

		// Calculate content checksum and set it to document object
		File tempFile = TempFilesHelper.toTempFile(file);
		try {
			String checksum = HashingHelper.sha256Hex(tempFile);
			document.setChecksum(checksum);

			try (var in = new FileInputStream(tempFile)) {
				document = documentService.uploadDocument(document, in);
			}
		} finally {
			TempFilesHelper.delete(tempFile);
		}

		URI location = tryBuildLocation("/api/content/documents/{id}", document);
		return (location != null) ? ResponseEntity.created(location).body(documentMaper.toDto(document))
				: ResponseEntity.status(201).body(documentMaper.toDto(document));
	}

	@Override
	public void downloadDocument(Long id, boolean inline, HttpServletResponse response) throws IOException {

		// Get metadata first (NO streaming yet)
		Document document = documentService.get(id)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

		// Set headers BEFORE writing body
		response.setContentType(document.getContentType());
		response.setHeader("Content-Disposition",
				(inline ? "inline" : "attachment") + "; filename=\"" + document.getOriginalFileName() + "\"");

		// Now stream content
		try (OutputStream out = response.getOutputStream()) {
			documentService.download(id, out);
		}

	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<Void> deleteDocument(Long id) {
		documentService.delete(id);
		return ResponseEntity.status(200).body(null);
	}

	@Override
	@RolesAllowed({ Roles.TENANT_ADMIN, Roles.ADMIN })
	public ResponseEntity<Void> startIngestion(Long id) {
		ingestionService.ingest(id);
		return ResponseEntity.status(200).body(null);
	}

	/**
	 * Best-effort Location header creator. Works if your domain objects expose
	 * getId(). If your Topic/Document uses a different ID getter, adjust this
	 * method.
	 */
	private URI tryBuildLocation(String pathTemplate, Object entity) {
		try {
			var getId = entity.getClass().getMethod("getId");
			Object id = getId.invoke(entity);
			if (id == null)
				return null;

			return ServletUriComponentsBuilder.fromCurrentContextPath().path(pathTemplate).buildAndExpand(id).toUri();
		} catch (Exception ignored) {
			return null;
		}
	}
}
