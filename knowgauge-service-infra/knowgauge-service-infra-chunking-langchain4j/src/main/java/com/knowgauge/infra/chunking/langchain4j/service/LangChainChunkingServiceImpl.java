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
			if (policy.isTrimWhitespace() && pageText != null) {
				pageText = pageText.trim();
			}
			if (pageText == null)
				pageText = "";

			int pageNumber = pageIndex + 1;

			List<TextSegment> segments = splitter.split(new DefaultDocument(pageText));

			int cursor = 0;

			for (int segIndex = 0; segIndex < segments.size(); segIndex++) {

				TextSegment segment = segments.get(segIndex);

				String chunkText = segment.text();
				if (policy.isTrimWhitespace() && chunkText != null) {
					chunkText = chunkText.trim();
				}
				if (chunkText == null)
					chunkText = "";

				int start = pageText.indexOf(chunkText, cursor);
				if (start == -1) {
					start = cursor; // best-effort fallback if not found
				}

				int end = start + chunkText.length();

				// By default chunk lives entirely within the same page
				Integer startPage = policy.isIncludePageMetadata() ? pageNumber : null;
				Integer endPage = policy.isIncludePageMetadata() ? pageNumber : null;
				int endOffset = end; // offset within endPage (same page by default)

				// Cross-page overlap: extend the LAST chunk of the current page
				// by taking overlapSizeChars from the NEXT page prefix.
				boolean isLastSegmentOnPage = (segIndex == segments.size() - 1);
				boolean hasNextPage = (pageIndex + 1 < pages.size());
				int overlap = Math.max(0, policy.getOverlapSizeChars());

				if (isLastSegmentOnPage && hasNextPage && overlap > 0) {

					String nextPageText = pages.get(pageIndex + 1);
					if (policy.isTrimWhitespace() && nextPageText != null) {
						nextPageText = nextPageText.trim();
					}
					if (nextPageText == null)
						nextPageText = "";

					int overlapLen = Math.min(overlap, nextPageText.length());
					if (overlapLen > 0) {
						String nextPrefix = nextPageText.substring(0, overlapLen);

						// Append next-page prefix to last chunk
						chunkText = chunkText + nextPrefix;

						// Metadata now spans into next page
						if (policy.isIncludePageMetadata()) {
							endPage = pageNumber + 1;
						}
						endOffset = overlapLen; // offset within NEXT page
					}
				}

				DocumentChunk chunk = new DocumentChunk(tenantId, topicId, documentId, version, null, ++globalOrdinal,
						chunkText, startPage, endPage, start, endOffset, HashingHelper.sha256Hex(chunkText));

				chunks.add(chunk);

				cursor = end; // keep cursor within current page
			}
		}

		return chunks;
	}

	private DocumentSplitter createSplitter(ChunkingPolicy policy) {
		return DocumentSplitters.recursive(policy.getMaxChunkSizeChars(), policy.getOverlapSizeChars());
	}
}
