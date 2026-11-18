package com.knowgauge.core.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
	private static final long OTHER_TENANT_ID = 9L;

	private DocumentService documentService;
	private IngestionService ingestionService;
	private ScheduledIngestionJob job;
	/** Tenant the context was switched to for each ingest call, in call order. */
	private final List<Long> tenantsAtIngest = new ArrayList<>();
	private Long currentTenant;

	@BeforeEach
	void setUp() {
		documentService = mock(DocumentService.class);
		ingestionService = mock(IngestionService.class);
		ExecutionContext executionContext = mock(ExecutionContext.class);
		// Jobs run as the system user (ExecutionContext.runAsSystem), so only that method is stubbed.
		doAnswer(inv -> {
			currentTenant = inv.getArgument(0);
			try {
				((Runnable) inv.getArgument(1)).run();
			} finally {
				currentTenant = null;
			}
			return null;
		}).when(executionContext).runAsSystem(anyLong(), any(Runnable.class));
		doAnswer(inv -> {
			tenantsAtIngest.add(currentTenant);
			return null;
		}).when(ingestionService).ingest(anyLong());
		when(documentService.findTenantIdsWithUploaded()).thenReturn(List.of(TENANT_ID));

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
	void ingestsEachTenantsDocumentsWithThatTenantAsTheContext() {
		when(documentService.findTenantIdsWithUploaded()).thenReturn(List.of(TENANT_ID, OTHER_TENANT_ID));
		when(documentService.findOldestUploaded(TENANT_ID, 3)).thenReturn(List.of(doc(11L)));
		when(documentService.findOldestUploaded(OTHER_TENANT_ID, 3)).thenReturn(List.of(doc(21L), doc(22L)));

		job.run();

		verify(ingestionService).ingest(11L);
		verify(ingestionService).ingest(21L);
		verify(ingestionService).ingest(22L);
		assertThat(tenantsAtIngest).containsExactly(TENANT_ID, OTHER_TENANT_ID, OTHER_TENANT_ID);
	}

	@Test
	void doesNothingWhenNoTenantHasUploadedDocuments() {
		when(documentService.findTenantIdsWithUploaded()).thenReturn(List.of());

		job.run();

		verify(documentService, never()).findOldestUploaded(anyLong(), org.mockito.ArgumentMatchers.anyInt());
		verify(ingestionService, never()).ingest(anyLong());
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
