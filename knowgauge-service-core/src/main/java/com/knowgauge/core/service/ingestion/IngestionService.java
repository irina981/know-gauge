package com.knowgauge.core.service.ingestion;

public interface IngestionService {
	void ingest(Long documentId) throws Exception;
}
