package com.knowgauge.core.service.ingestion;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.DocumentIngestionConflictException;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.service.content.DocumentService;

import lombok.extern.slf4j.Slf4j;

/**
 * Periodically ingests the oldest {@code UPLOADED} documents of the current
 * tenant, up to {@code kg.ingestion.job.batch-size} per run, on the
 * {@code kg.ingestion.job.cron} schedule.
 * <p>
 * Documents are ingested one after another through {@link IngestionService}, which
 * claims each one with a conditional status update, so a document that a REST
 * call or another instance is already ingesting is skipped. {@code FAILED}
 * documents are not picked up: they are retried through the REST endpoint.
 * Spring does not start a cron run while the previous one is still running.
 */
@Component
@ConditionalOnProperty(name = "kg.ingestion.job.enabled", havingValue = "true")
@Slf4j
public class ScheduledIngestionJob {

	private final DocumentService documentService;
	private final IngestionService ingestionService;
	private final ExecutionContext executionContext;
	private final IngestionJobProperties properties;

	public ScheduledIngestionJob(DocumentService documentService, IngestionService ingestionService,
			ExecutionContext executionContext, IngestionJobProperties properties) {
		this.documentService = documentService;
		this.ingestionService = ingestionService;
		this.executionContext = executionContext;
		this.properties = properties;
		log.info("Scheduled ingestion job enabled: cron='{}', batchSize={}", properties.getCron(),
				properties.getBatchSize());
	}

	@Scheduled(cron = "${kg.ingestion.job.cron}")
	public void run() {
		long tenantId = executionContext.tenantId();
		List<Document> batch = documentService.findOldestUploaded(tenantId, properties.getBatchSize());
		if (batch.isEmpty()) {
			log.debug("Scheduled ingestion: no UPLOADED documents for tenant {}", tenantId);
			return;
		}

		log.info("Scheduled ingestion: {} UPLOADED document(s) picked for tenant {}", batch.size(), tenantId);
		int ingested = 0;
		int skipped = 0;
		int failed = 0;
		for (Document document : batch) {
			Long documentId = document.getId();
			try {
				ingestionService.ingest(documentId);
				ingested++;
			} catch (DocumentIngestionConflictException ex) {
				skipped++;
				log.info("Scheduled ingestion: document {} skipped - {}", documentId, ex.getMessage());
			} catch (RuntimeException ex) {
				// IngestionService has already marked the document FAILED with the error message.
				failed++;
				log.error("Scheduled ingestion: document {} failed", documentId, ex);
			}
		}
		log.info("Scheduled ingestion finished for tenant {}: {} ingested, {} skipped, {} failed", tenantId,
				ingested, skipped, failed);
	}
}
