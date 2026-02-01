package com.knowgauge.restapi.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.knowgauge.contract.dto.DocumentInput;
import com.knowgauge.contract.dto.TopicInput;
import com.knowgauge.contract.dto.TopicTreeNodeInput;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.service.content.ContentService;
import com.knowgauge.restapi.mapper.DocumentMapper;
import com.knowgauge.restapi.mapper.TopicMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/content")
public class ContentController {

	private final ContentService contentService;
	private final TopicMapper topicMapper;
	private final DocumentMapper documentMaper;

	public ContentController(ContentService contentService, TopicMapper topicMapper, DocumentMapper documentMaper) {
		this.contentService = contentService;
		this.topicMapper = topicMapper;
		this.documentMaper = documentMaper;
	}

	// -----------------------------
	// Topics
	// -----------------------------

	@PostMapping("/topics")
	public ResponseEntity<Topic> createTopic(@RequestBody @Valid TopicInput topicInput) {
		Topic created = contentService.createTopic(topicMapper.toDomain(topicInput));

		// If Topic has an ID, you can return Location header.
		// Otherwise, OK to just return 201 with body.
		URI location = tryBuildLocation("/api/content/topics/{id}", created);
		return (location != null) ? ResponseEntity.created(location).body(created)
				: ResponseEntity.status(201).body(created);
	}

	@PostMapping("/topics/tree")
	public ResponseEntity<Topic> createTopicTree(@RequestBody @Valid TopicTreeNodeInput topicTreeNodeInput) {
		Topic createdRoot = contentService.createTopicTree(topicMapper.toDomain(topicTreeNodeInput));

		URI location = tryBuildLocation("/api/content/topics/{id}", createdRoot);
		return (location != null) ? ResponseEntity.created(location).body(createdRoot)
				: ResponseEntity.status(201).body(createdRoot);
	}

	// -----------------------------
	// Documents
	// -----------------------------

	/**
	 * Expects multipart/form-data with: - part "meta": JSON for DocumentInput -
	 * part "file": the actual PDF (or any file)
	 */
	@PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Document> uploadDocument(@RequestPart("meta") @Valid DocumentInput documentInput,
			@RequestPart("file") MultipartFile file) throws IOException {

		// Basic defensive checks (optional but recommended)
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		Document document = documentMaper.toDomain(documentInput, file);


		// IMPORTANT: try-with-resources is fine IF uploadDocument reads the stream
		// fully inside the call.
		// (Which it should, because the controller must not keep streams open after
		// returning.)
		try (InputStream in = file.getInputStream()) {
			document = contentService.uploadDocument(document, in);
		}

		URI location = tryBuildLocation("/api/content/documents/{id}", document);
		return (location != null) ? ResponseEntity.created(location).body(document)
				: ResponseEntity.status(201).body(document);
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
