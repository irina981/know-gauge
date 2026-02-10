package com.knowgauge.core.service.ingestion;

import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.ingestion.ChunkingService;
import com.knowgauge.core.port.ingestion.EmbeddingService;
import com.knowgauge.core.port.ingestion.PageExtractionService;
import com.knowgauge.core.port.ingestion.TextSplittingService;
import com.knowgauge.core.service.content.DocumentService;

@Service
public class IngestionServiceImpl implements IngestionService {

	private final PageExtractionService pageExtractionService;
	private final TextSplittingService textSplittingService;
	private final ChunkingService chunkingService;
	private final EmbeddingService embeddingService;
	private final DocumentService documentService;
	private final IngestionTransactionalServiceImpl ingestionTransactionalService;

	public IngestionServiceImpl(PageExtractionService pageExtractionService, TextSplittingService textSplittingService,
			ChunkingService chunkingService, DocumentService documentService,
			IngestionTransactionalServiceImpl ingestionTransactionalService, EmbeddingService embeddingService) {
		this.pageExtractionService = pageExtractionService;
		this.textSplittingService = textSplittingService;
		this.chunkingService = chunkingService;
		this.embeddingService = embeddingService;
		this.documentService = documentService;
		this.ingestionTransactionalService = ingestionTransactionalService;
	}

	@Override
	public void ingest(Long documentId) {
		// 1) Check if document exists and is in state of UPLADED
		Document document = documentService.get(documentId)
				.orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

		if (document.getStatus() != DocumentStatus.UPLOADED) {
			throw new IllegalArgumentException("Document not in UPLOADED state.");
		}

		Long tenantId = document.getTenantId();
		Integer documentVersion = document.getVersion();
		String embeddingModel = embeddingService.modelName();

		// 2) Update document status from UPLOADED to INGESTING
		ingestionTransactionalService.updateStatusIfCurrent(documentId, DocumentStatus.UPLOADED,
				DocumentStatus.INGESTING);

		try {
			List<String> pages;
			// 3) Load document content from storage
			try (InputStream in = documentService.download(documentId)) {
				// 4) Extract document text into pages
				pages = pageExtractionService.extractPages(in);
			}

			// 5) Split each page into chunks 
			List<DocumentChunk> chunks = chunkingService.chunkDocument(tenantId, documentId, documentVersion, pages,
					textSplittingService);

			// 6) Replace old chunks (if exist) with new ones in repository
			ingestionTransactionalService.replaceChunks(tenantId, documentId, documentVersion, chunks, embeddingModel);
			
			// 7) Generate embeddings for chunks and persist them
			// TODO embeddingService.embed(chunks);

			// 8) Mark document as INGESTED
			ingestionTransactionalService.markIngested(documentId);

		} catch (Exception ex) {
			// 9) Mark document as FAILED with error message persisted
			ingestionTransactionalService.markFailed(documentId,
					"Ingestion failed for document " + documentId + ": " + ex.getMessage());
			throw new RuntimeException("Ingestion failed for document " + documentId, ex);
		}
	}
}
