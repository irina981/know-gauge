package com.knowgauge.core.service.ingestion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.port.repository.ChunkEmbeddingRepository;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.service.content.DocumentService;

@Service
@Transactional
public class IngestionTransactionalServiceImpl {

	private final DocumentService documentService;
	private final DocumentChunkRepository documentChunkRepository;
	private final ChunkEmbeddingRepository chunkEmbeddingRepository;

	public IngestionTransactionalServiceImpl(DocumentService documentService,
			DocumentChunkRepository documentChunkRepository, ChunkEmbeddingRepository chunkEmbeddingRepository) {
		this.documentService = documentService;
		this.documentChunkRepository = documentChunkRepository;
		this.chunkEmbeddingRepository = chunkEmbeddingRepository;
	}

	/**
	 * Status updates should commit independently of the main ingestion flow.
	 */
	public void markIngested(Long documentId) {
		documentService.markIngested(documentId);
	}

	public void markFailed(Long documentId, String errorMessage) {
		documentService.markFailed(documentId, errorMessage);
	}

	public void markIngesting(Long documentId) {
		documentService.markIngesting(documentId);
	}

	/**
	 * Atomically replaces all chunks (and embeddings) for a document version.
	 */
	public List<DocumentChunk> persistChunks(Long tenantId, Long documentId, Integer documentVersion,
			List<DocumentChunk> chunks, String embeddingModel) {

		// Delete old chunks first
		documentChunkRepository.deleteByTenantIdAndDocumentIdAndDocumentVersion(tenantId, documentId, documentVersion);

		// Persist new chunks
		List<DocumentChunk> savedChunks = documentChunkRepository.saveAll(chunks);

		return savedChunks;
	}
}
