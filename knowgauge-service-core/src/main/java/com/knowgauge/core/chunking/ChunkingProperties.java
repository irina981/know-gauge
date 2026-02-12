package com.knowgauge.core.chunking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kg.chunking")
public class ChunkingProperties {

	private int maxChunkSizeChars;
	private int overlapSizeChars;
	private boolean trimWhitespace;
	private boolean includePageMetadata;

	public int getMaxChunkSizeChars() {
		return maxChunkSizeChars;
	}

	public void setMaxChunkSizeChars(int maxChunkSizeChars) {
		this.maxChunkSizeChars = maxChunkSizeChars;
	}

	public int getOverlapSizeChars() {
		return overlapSizeChars;
	}

	public void setOverlapSizeChars(int overlapSizeChars) {
		this.overlapSizeChars = overlapSizeChars;
	}

	public boolean isTrimWhitespace() {
		return trimWhitespace;
	}

	public void setTrimWhitespace(boolean trimWhitespace) {
		this.trimWhitespace = trimWhitespace;
	}

	public boolean isIncludePageMetadata() {
		return includePageMetadata;
	}

	public void setIncludePageMetadata(boolean includePageMetadata) {
		this.includePageMetadata = includePageMetadata;
	}
}
