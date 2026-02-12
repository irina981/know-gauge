package com.knowgauge.core.port.ingestion;

import java.util.List;

import com.knowgauge.core.model.DocumentChunk;

public interface ChunkingService {
	List<DocumentChunk> chunkDocument(Long tenantId, Long topicId, Long documentId, Integer version, List<String> pages);
}
