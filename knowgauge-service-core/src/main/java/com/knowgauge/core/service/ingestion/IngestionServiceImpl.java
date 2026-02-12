package com.knowgauge.core.service.ingestion;

import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.chunking.ChunkingPolicy;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.documentparser.DocumentParser;
import com.knowgauge.core.port.embedding.EmbeddingService;
import com.knowgauge.core.service.chunking.ChunkingService;
import com.knowgauge.core.service.content.DocumentService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {

	private final List<DocumentParser> pageExtractionServices;
	private final ChunkingService chunkingService;
	private final EmbeddingService embeddingService;
	private final DocumentService documentService;
	private final IngestionTransactionalServiceImpl ingestionTransactionalService;
	private final IngestionVectorTransactionalServiceImpl ingestionVectorTransactionalService;
	private final ChunkingPolicy chunkingPolicy;

	public IngestionServiceImpl(List<DocumentParser> pageExtractionServices,
			ChunkingService chunkingService, DocumentService documentService,
			IngestionTransactionalServiceImpl ingestionTransactionalService, EmbeddingService embeddingService, IngestionVectorTransactionalServiceImpl ingestionVectorTransactionalService, ChunkingPolicy chunkingPolicy) {
		this.pageExtractionServices = pageExtractionServices;
		this.chunkingService = chunkingService;
		this.embeddingService = embeddingService;
		this.documentService = documentService;
		this.ingestionTransactionalService = ingestionTransactionalService;
		this.ingestionVectorTransactionalService = ingestionVectorTransactionalService;
		this.chunkingPolicy = chunkingPolicy;
	}

	@Override
	public void ingest(Long documentId) {
		log.info("*** Ingestion started for document: {}", documentId);
		
		// 1) Check if document exists and is in state of UPLADED
		Document document = documentService.get(documentId)
				.orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

		if (document.getStatus() != DocumentStatus.UPLOADED && document.getStatus() != DocumentStatus.FAILED) {
			throw new IllegalArgumentException("Document not in UPLOADED nor FAILED state.");
		}

		Long tenantId = document.getTenantId();
		Long topicId = document.getTopicId();
		Integer documentVersion = document.getVersion();
		String embeddingModel = embeddingService.modelName();

		// 2) Update document status from UPLOADED to INGESTING
		ingestionTransactionalService.markIngesting(documentId);
		log.info("       Ingesting document {} - Status updated from {} to {}", documentId, DocumentStatus.UPLOADED, DocumentStatus.INGESTING);

		try {
			List<String> pages;
			// 3) Load document content from storage
			try (InputStream in = documentService.download(documentId)) {
				// 4) Extract document text into pages
				DocumentParser pageExtractionService = getPageExtractionService(document.getContentType());
				pages = pageExtractionService.extractPages(in);
				log.info("   Ingesting document {} - Content extracted to {} pages.", documentId, pages.size());
			}

			// 5) Split each page into chunks
			List<DocumentChunk> chunks = chunkingService.chunkDocument(tenantId, topicId, documentId, documentVersion, pages, chunkingPolicy);
			log.info("   Ingesting document {} - Pages devided into {} chunks.", documentId, chunks.size());

			// 6) Replace old chunks (if exist) with new ones in repository
			List<DocumentChunk> savedChunks = ingestionTransactionalService.persistChunks(tenantId, documentId, documentVersion, chunks, embeddingModel);
			log.info("   Ingesting document {} - {} chunks persisted", documentId, chunks.size());

			// 7) Generate embeddings for chunks
			log.info("   EmbeddingService impl = {}", embeddingService.getClass().getName());
			log.info("   Embedding model name = {}", embeddingService.modelName());
			List<float[]> vectors = embeddingService.embed(chunks.stream().map(chunk -> chunk.getChunkText()).toList());
			log.info("   Ingesting document {} - {} chunks embedded", documentId, chunks.size());
			
			// 8) Persist generated embeddings
			ingestionVectorTransactionalService.persistEmbeddings(tenantId, documentId, documentVersion, savedChunks, vectors, embeddingModel);
			log.info("   Ingesting document {} - {} embeddings persisted", documentId, chunks.size());

			// 9) Mark document as INGESTED
			ingestionTransactionalService.markIngested(documentId);
			log.info("      Ingesting document {} - Marked as {}", documentId, DocumentStatus.INGESTED);

		} catch (Exception ex) {
			// 10) Mark document as FAILED with error message persisted
			ingestionTransactionalService.markFailed(documentId,
					"Ingestion failed for document " + documentId + ": " + ex.getMessage());
			throw new RuntimeException("Ingestion failed for document " + documentId, ex);
		} 
		
		log.info("*** Ingestion ended for document: {}", documentId);
		
	}

	private DocumentParser getPageExtractionService(String contentType) {
		return pageExtractionServices.stream().filter(service -> contentType.equals(service.contentType()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException(String.format("Content type {} not supported", contentType)));
	}
}
