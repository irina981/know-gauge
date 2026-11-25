package com.knowgauge.contract.api;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.knowgauge.contract.dto.DocumentDto;
import com.knowgauge.contract.dto.TopicCreateInput;
import com.knowgauge.contract.dto.TopicDocumentCountDto;
import com.knowgauge.contract.dto.TopicDto;
import com.knowgauge.contract.dto.TopicTreeNodeInput;
import com.knowgauge.contract.dto.TopicUpdateInput;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/** Topics and their source documents. */
@RequestMapping("/api/content")
@Tag(name = "Content", description = "Manage topics and source documents")
public interface ContentApi {

	// -----------------------------
	// Topics
	// -----------------------------

	@GetMapping("/topics/{id}")
	@Operation(summary = "Get topic by id")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Topic returned"),
			@ApiResponse(responseCode = "404", description = "Topic not found") })
	ResponseEntity<TopicDto> getTopic(@PathVariable Long id);

	@GetMapping("/topics/roots")
	@Operation(summary = "List root topics")
	ResponseEntity<List<TopicDto>> getRoots();

	@GetMapping("/topics/children/{parentId}")
	@Operation(summary = "List children topics")
	ResponseEntity<List<TopicDto>> getChildren(@PathVariable Long parentId);

	@GetMapping("/topics/ingested-document-counts")
	@Operation(summary = "Count ingested documents per topic", description = "For each topic that directly holds ingested documents, how many it holds. Topics without any are left out; add up a subtree to know whether a parent topic can be used for test generation.")
	ResponseEntity<List<TopicDocumentCountDto>> getIngestedDocumentCounts();

	@GetMapping("/topics/descendants/{parentId}")
	@Operation(summary = "List all descendant topics")
	ResponseEntity<List<TopicDto>> getDescendants(@PathVariable Long parentId);

	@DeleteMapping("/topics/{id}")
	@Operation(summary = "Delete topic")
	ResponseEntity<Void> deleteTopic(@PathVariable Long id);

	@PostMapping("/topics")
	@Operation(summary = "Create topic")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Topic created") })
	ResponseEntity<TopicDto> createTopic(@RequestBody @Valid TopicCreateInput topicInput);

	@PutMapping("/topics/{id}")
	@Operation(summary = "Update topic name and description", description = "Null fields are left unchanged. The id in the body, if any, is ignored.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Topic updated"),
			@ApiResponse(responseCode = "400", description = "Invalid input or duplicate name under the same parent"),
			@ApiResponse(responseCode = "404", description = "Topic not found") })
	ResponseEntity<TopicDto> updateTopic(@PathVariable Long id, @RequestBody @Valid TopicUpdateInput topicInput);

	@PostMapping("/topics/tree")
	@Operation(summary = "Create topic tree")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Topic tree created") })
	ResponseEntity<TopicDto> createTopicTree(@RequestBody @Valid TopicTreeNodeInput topicTreeNodeInput);

	// -----------------------------
	// Documents
	// -----------------------------

	@GetMapping("/documents/{id}")
	@Operation(summary = "Get document metadata by id")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Document returned"),
			@ApiResponse(responseCode = "404", description = "Document not found") })
	ResponseEntity<DocumentDto> getDocument(@PathVariable Long id);

	@GetMapping("/documents/all/{topicId}")
	@Operation(summary = "List documents for topic (paged)", description = "In upload order (by id) unless a sort is given.")
	ResponseEntity<Page<DocumentDto>> getAllDocuments(@PathVariable Long topicId,
			@PageableDefault(sort = "id") Pageable pageable);

	/**
	 * Expects multipart/form-data with: - part "meta": JSON for DocumentInput -
	 * part "file": the actual PDF (or any file)
	 */
	@PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload document", description = "Multipart upload with meta JSON and file binary parts")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Document uploaded"),
			@ApiResponse(responseCode = "400", description = "Invalid upload payload") })
	ResponseEntity<DocumentDto> uploadDocument(@RequestPart("meta") @Valid String metaJson,
			@RequestPart("file") MultipartFile file) throws IOException;

	@GetMapping("/documents/{id}/content")
	@Operation(summary = "Download document content", description = "With inline=true the browser shows the file (for example in a PDF preview) instead of saving it.")
	void downloadDocument(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean inline,
			HttpServletResponse response) throws IOException;

	@DeleteMapping("/documents/{id}")
	@Operation(summary = "Delete document")
	ResponseEntity<Void> deleteDocument(@PathVariable Long id);

	@PostMapping("/documents/ingestion/{id}")
	@Operation(summary = "Start ingestion for document")
	ResponseEntity<Void> startIngestion(@PathVariable Long id);
}
