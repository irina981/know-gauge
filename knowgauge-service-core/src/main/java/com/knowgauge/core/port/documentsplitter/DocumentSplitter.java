package com.knowgauge.core.port.documentsplitter;

import java.util.List;

import com.knowgauge.core.service.chunking.ChunkingPolicy;

public interface DocumentSplitter {
	List<String> split(String text, ChunkingPolicy policy);
}
