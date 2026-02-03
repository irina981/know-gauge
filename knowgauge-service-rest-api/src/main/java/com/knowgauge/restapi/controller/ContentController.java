package com.knowgauge.restapi.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowgauge.contract.dto.DocumentDto;
import com.knowgauge.contract.dto.DocumentInput;
import com.knowgauge.contract.dto.TopicCreateInput;
import com.knowgauge.contract.dto.TopicDto;
import com.knowgauge.contract.dto.TopicTreeNodeInput;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;
import com.knowgauge.restapi.mapper.DocumentMapper;
import com.knowgauge.restapi.mapper.TopicMapper;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/content")
public class ContentController {

	private final DocumentService documentService;
	private final TopicService topicService;
	private final TopicMapper topicMapper;
	private final DocumentMapper documentMaper;

	public ContentController(DocumentService documentService, TopicService topicService, TopicMapper topicMapper,
			DocumentMapper documentMaper) {
		this.documentService = documentService;
		this.topicService = topicService;
		this.documentMaper = documentMaper;
		this.topicMapper = topicMapper;
	}

	// -----------------------------
	// Topics
	// -----------------------------

	@GetMapping("/topics/{id}")
	public ResponseEntity<TopicDto> getById(@PathVariable Long id) {
		Topic topic = topicService.findById(id).orElse(null);
		return ResponseEntity.status(200).body(topicMapper.toDto(topic));
	}

	@GetMapping("/topics/roots")
	public ResponseEntity<List<TopicDto>> getRoots() {
		List<TopicDto> topics = topicService.getRoots().stream().map(topicMapper::toDto).toList();
		return ResponseEntity.status(200).body(topics);
	}
	
	@GetMapping("/topics/children/{parentId}")
	public ResponseEntity<List<TopicDto>> getChildren(@PathVariable Long parentId) {
		List<TopicDto> topics = topicService.getChildren(parentId).stream().map(topicMapper::toDto).toList();
		return ResponseEntity.status(200).body(topics);
	}
	
	@GetMapping("/topics/descendants/{parentId}")
	public ResponseEntity<List<TopicDto>> getDescendants(@PathVariable Long parentId) {
		List<TopicDto> topics = topicService.getDescendants(parentId).stream().map(topicMapper::toDto).toList();
		return ResponseEntity.status(200).body(topics);
	}

	@DeleteMapping("/topics/{id}")
	public ResponseEntity<TopicDto> delete(@PathVariable Long id) {
		topicService.delete(id);
		return ResponseEntity.status(200).body(null);
	}

	@PostMapping("/topics")
	public ResponseEntity<TopicDto> createTopic(@RequestBody @Valid TopicCreateInput topicInput) {
		Topic created = topicService.create(topicMapper.toDomain(topicInput));

		// If Topic has an ID, you can return Location header.
		// Otherwise, OK to just return 201 with body.
		URI location = tryBuildLocation("/api/content/topics/{id}", created);
		return (location != null) ? ResponseEntity.created(location).body(topicMapper.toDto(created))
				: ResponseEntity.status(201).body(topicMapper.toDto(created));
	}

	@PostMapping("/topics/tree")
	public ResponseEntity<TopicDto> createTopicTree(@RequestBody @Valid TopicTreeNodeInput topicTreeNodeInput) {
		Topic createdRoot = topicService.createTopicTree(topicMapper.toDomain(topicTreeNodeInput));

		URI location = tryBuildLocation("/api/content/topics/{id}", createdRoot);
		return (location != null) ? ResponseEntity.created(location).body(topicMapper.toDto(createdRoot))
				: ResponseEntity.status(201).body(topicMapper.toDto(createdRoot));
	}

	// -----------------------------
	// Documents
	// -----------------------------

	/**
	 * Expects multipart/form-data with: - part "meta": JSON for DocumentInput -
	 * part "file": the actual PDF (or any file)
	 */
	@PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentDto> uploadDocument(@RequestPart("meta") @Valid String metaJson,
			@RequestPart("file") MultipartFile file) throws IOException {
		
		ObjectMapper mapper = new ObjectMapper();
	    DocumentInput documentInput = mapper.readValue(metaJson, DocumentInput.class);

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
			document = documentService.uploadDocument(document, in);
		}

		URI location = tryBuildLocation("/api/content/documents/{id}", document);
		return (location != null) ? ResponseEntity.created(location).body(documentMaper.toDto(document))
				: ResponseEntity.status(201).body(documentMaper.toDto(document));
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
