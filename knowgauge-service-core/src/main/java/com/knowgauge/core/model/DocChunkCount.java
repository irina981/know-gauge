package com.knowgauge.core.model;

/**
 * Holds document-level corpus stats after applying the active filters: -
 * documentId: which document the chunk belongs to - chunkCount: how many
 * embedding rows are available for that document in the scope
 *
 * Used to calculate per-document quotas (balanced selection).
 */
public class DocChunkCount {
	public final Long documentId;
	public final long chunkCount;

	public DocChunkCount(Long documentId, long chunkCount) {
		this.documentId = documentId;
		this.chunkCount = chunkCount;
	}
	
	
}