package com.knowgauge.core.service.chunking;

import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Defines how documents are split into chunks. This affects embedding quality,
 * retrieval accuracy, and reproducibility.
 */
@Component
@Data
public final class ChunkingPolicy {

	private final int maxChunkSizeChars;
	private final int overlapSizeChars;
	private final boolean trimWhitespace;
	private final boolean includePageMetadata;

	private ChunkingPolicy(ChunkingProperties props) {
		this.maxChunkSizeChars = props.getMaxChunkSizeChars();
		this.overlapSizeChars = props.getOverlapSizeChars();
		this.trimWhitespace = props.isTrimWhitespace();
		this.includePageMetadata = props.isIncludePageMetadata();
	}
}
