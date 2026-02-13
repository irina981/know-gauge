package com.knowgauge.infra.chunking.langchain4j.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.chunking.ChunkingPolicy;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.service.chunking.ChunkingService;
import com.knowgauge.core.util.HashingHelper;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

@Service
public class LangChainChunkingServiceImpl implements ChunkingService {

	@Override
	public List<DocumentChunk> chunkDocument(Long tenantId, Long topicId, Long documentId, Integer version,
			List<String> pages, ChunkingPolicy policy) {

		DocumentSplitter splitter = createSplitter(policy);

		List<DocumentChunk> chunks = new ArrayList<>();
		int globalOrdinal = 0;

		for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {

			String pageText = pages.get(pageIndex);

			if (policy.isTrimWhitespace()) {
				pageText = pageText.trim();
			}

			int pageNumber = pageIndex + 1;

			List<TextSegment> segments = splitter.split(new DefaultDocument(pageText));

			int cursor = 0;

			for (TextSegment segment : segments) {

				String chunkText = segment.text();

				if (policy.isTrimWhitespace()) {
					chunkText = chunkText.trim();
				}

				int start = pageText.indexOf(chunkText, cursor);

				if (start == -1) {
					start = cursor;
				}

				int end = start + chunkText.length();

				DocumentChunk chunk = new DocumentChunk(tenantId, topicId, documentId, version, null, ++globalOrdinal,
						chunkText, policy.isIncludePageMetadata() ? pageNumber : null,
						policy.isIncludePageMetadata() ? pageNumber : null, start, end,
						HashingHelper.sha256Hex(chunkText));

				chunks.add(chunk);

				cursor = end;
			}
		}

		return chunks;
	}

	private DocumentSplitter createSplitter(ChunkingPolicy policy) {

		return DocumentSplitters.recursive(policy.getMaxChunkSizeChars(), policy.getOverlapSizeChars());
	}
}
