package com.knowgauge.infra.documentsplitting.langchain4j.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.service.chunking.ChunkingPolicy;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

@Service
public class LangChainDocumentSplitter implements com.knowgauge.core.port.documentsplitter.DocumentSplitter {

	private DocumentSplitter createSplitter(ChunkingPolicy policy) {
		return DocumentSplitters.recursive(policy.getMaxChunkSizeChars(), policy.getOverlapSizeChars());
	}

	@Override
	public List<String> split(String text, ChunkingPolicy policy) {
		DocumentSplitter splitter = createSplitter(policy);

		return splitter.split(new DefaultDocument(text)).stream().map(TextSegment::text).toList();
	}
}
