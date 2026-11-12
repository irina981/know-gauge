package com.knowgauge.core.exception;

/**
 * The document could not be claimed for ingestion because it is no longer in
 * {@code UPLOADED} or {@code FAILED} state, typically because another request or
 * the scheduled ingestion job is already ingesting it. Maps to HTTP 409.
 */
public class DocumentIngestionConflictException extends BaseException {

	private static final long serialVersionUID = 1L;

	public DocumentIngestionConflictException(Long documentId) {
		super("Document " + documentId + " is already being ingested or is no longer UPLOADED/FAILED");
	}
}
