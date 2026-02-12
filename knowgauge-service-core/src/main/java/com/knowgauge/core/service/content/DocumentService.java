package com.knowgauge.core.service.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.enums.DocumentStatus;

public interface DocumentService {

	/**
	 * Uploads a document and stores metadata + objectKey. Ingestion/chunking can be
	 * triggered later (separate endpoint/job).
	 */
	Document uploadDocument(Document document, InputStream contentStream);

	Optional<Document> get(Long id);

	Document download(Long id, OutputStream out);

	InputStream download(Long id);

	void delete(Long topicId);

	Page<Document> getAllDocuments(Long topicId, Pageable pageable);

	int markIngesting(Long documentId);

	int markIngested(Long documentId);

	int markFailed(Long documentId, String errorMessage);
}
