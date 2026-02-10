package com.knowgauge.core.service.ingestion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.repository.ChunkEmbeddingRepository;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.service.content.DocumentService;

@Service
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
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markIngested(Long documentId) {
		documentService.markIngested(documentId);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long documentId, String errorMessage) {
		documentService.markFailed(documentId, errorMessage);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateStatusIfCurrent(Long documentId, DocumentStatus fromStatus, DocumentStatus toStatus) {
		documentService.updateStatusIfCurrent(documentId, fromStatus, toStatus);
	}

	/**
	 * Atomically replaces all chunks (and embeddings) for a document version.
	 */
	@Transactional
	public void replaceChunks(Long tenantId, Long documentId, Integer documentVersion, List<DocumentChunk> chunks,
			String embeddingModel) {

		// Delete old embeddings first (foreign key safety)
		chunkEmbeddingRepository.deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(tenantId, documentId,
				documentVersion, embeddingModel);

		// Delete old chunks
		documentChunkRepository.deleteByTenantIdAndDocumentIdAndDocumentVersion(tenantId, documentId, documentVersion);

		// Persist new chunks
		for (DocumentChunk chunk : chunks) {
			documentChunkRepository.save(chunk);
		}
	}
}
