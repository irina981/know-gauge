package com.knowgauge.core.service.ingestion;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.DocumentIngestionConflictException;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.service.content.DocumentService;

class ScheduledIngestionJobTest {

	private static final long TENANT_ID = 7L;

	private DocumentService documentService;
	private IngestionService ingestionService;
	private ScheduledIngestionJob job;

	@BeforeEach
	void setUp() {
		documentService = mock(DocumentService.class);
		ingestionService = mock(IngestionService.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		when(executionContext.tenantId()).thenReturn(TENANT_ID);

		IngestionJobProperties properties = new IngestionJobProperties();
		properties.setEnabled(true);
		properties.setCron("0 */5 * * * *");
		properties.setBatchSize(3);

		job = new ScheduledIngestionJob(documentService, ingestionService, executionContext, properties);
	}

	@Test
	void ingestsTheOldestUploadedDocumentsInOrder() {
		when(documentService.findOldestUploaded(TENANT_ID, 3)).thenReturn(List.of(doc(11L), doc(12L), doc(13L)));

		job.run();

		InOrder order = inOrder(ingestionService);
		order.verify(ingestionService).ingest(11L);
		order.verify(ingestionService).ingest(12L);
		order.verify(ingestionService).ingest(13L);
	}

	@Test
	void doesNothingWhenNoDocumentIsUploaded() {
		when(documentService.findOldestUploaded(TENANT_ID, 3)).thenReturn(List.of());

		job.run();

		verify(ingestionService, never()).ingest(anyLong());
	}

	@Test
	void continuesWithTheBatchWhenADocumentFailsOrIsAlreadyClaimed() {
		when(documentService.findOldestUploaded(TENANT_ID, 3)).thenReturn(List.of(doc(11L), doc(12L), doc(13L)));
		doThrow(new RuntimeException("embedding failed")).when(ingestionService).ingest(11L);
		doThrow(new DocumentIngestionConflictException(12L)).when(ingestionService).ingest(12L);

		job.run();

		verify(ingestionService).ingest(13L);
	}

	private static Document doc(Long id) {
		Document document = Document.builder().build();
		document.setId(id);
		return document;
	}
}
