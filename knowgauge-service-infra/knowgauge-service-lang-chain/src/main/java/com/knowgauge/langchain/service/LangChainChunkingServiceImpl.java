package com.knowgauge.langchain.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.port.ingestion.ChunkingService;
import com.knowgauge.core.util.HashingHelper;

import dev.langchain4j.data.document.DefaultDocument;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

@Service
public class LangChainChunkingServiceImpl implements ChunkingService {
	private final DocumentSplitter splitter;

	public LangChainChunkingServiceImpl(DocumentSplitter splitter) {
		this.splitter = splitter;
	}

	@Override
	public List<DocumentChunk> chunkDocument(Long tenantId, Long topicId, Long documentId, Integer version, List<String> pages) {
		List<DocumentChunk> chunks = new ArrayList<>();
        int globalOrdinal = 0;

        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            String pageText = pages.get(pageIndex);
            int pageNumber = pageIndex + 1;

            List<TextSegment> segments = splitter.split(new DefaultDocument(pageText));

            int cursor = 0;

            for (TextSegment segment : segments) {
                String chunkText = segment.text();

                // find chunk position in page text starting from cursor
                int start = pageText.indexOf(chunkText, cursor);
                if (start == -1) {
                    // fallback (rare, but defensive)
                    start = cursor;
                }
                int end = start + chunkText.length();

                chunks.add(new DocumentChunk(tenantId, topicId, documentId, version, null, ++globalOrdinal, chunkText, pageNumber, pageNumber, start, end, HashingHelper.sha256Hex(chunkText)));

                cursor = end;
            }
        }
        return chunks;
	}

}
