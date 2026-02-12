package com.knowgauge.core.service.ingestion;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.vectorstore.VectorStore;
import com.knowgauge.core.service.content.DocumentService;

@Service
@Transactional(transactionManager = "vectorTransactionManager")
public class IngestionVectorTransactionalServiceImpl {

	private final VectorStore vectorStore;

	public IngestionVectorTransactionalServiceImpl(DocumentService documentService,
			DocumentChunkRepository documentChunkRepository, VectorStore chunkEmbeddingRepository) {
		this.vectorStore = chunkEmbeddingRepository;
	}

	public List<ChunkEmbedding> persistEmbeddings(Long tenantId, Long documentId, Integer documentVersion,
			List<DocumentChunk> chunks, List<float[]> vectors, String embeddingModel) {
		if (chunks.size() != vectors.size()) {
			throw new IllegalStateException(
					"Embedding result size mismatch. chunks=" + chunks.size() + ", vectors=" + vectors.size());
		}

		// Delete old embeddings first
		vectorStore.deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(tenantId, documentId,
				documentVersion, embeddingModel);

		List<ChunkEmbedding> embeddings = IntStream.range(0, vectors.size()).mapToObj(i -> {
			DocumentChunk chunk = chunks.get(i);
			float[] vector = vectors.get(i);

			return ChunkEmbedding.builder().tenantId(chunk.getTenantId()).topicId(chunk.getTopicId())
					.documentId(chunk.getDocumentId()).documentVersion(chunk.getDocumentVersion())
					.sectionId(chunk.getSectionId()).chunkId(chunk.getId()).chunkChecksum(chunk.getChecksum())
					.embedding(vector).embeddingModel(embeddingModel).build();
		}).collect(Collectors.toList());

		List<ChunkEmbedding> savedEmbeddings = vectorStore.saveAll(embeddings);

		return savedEmbeddings;
	}
}
