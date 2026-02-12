package com.knowgauge.core.service.chunking;

import java.util.List;

import com.knowgauge.core.chunking.ChunkingPolicy;
import com.knowgauge.core.model.DocumentChunk;

public interface ChunkingService {
	List<DocumentChunk> chunkDocument(Long tenantId, Long topicId, Long documentId, Integer version, List<String> pages, ChunkingPolicy policy);
}
