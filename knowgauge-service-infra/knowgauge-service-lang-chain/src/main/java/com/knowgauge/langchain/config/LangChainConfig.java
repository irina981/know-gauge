package com.knowgauge.langchain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;

@Configuration
public class LangChainConfig {

	@Bean
	public DocumentSplitter getTextSplitter(@Value("${kg.chunking.maxSegmentSizeInChars}") int maxSegmentSizeInChars,
			@Value("${kg.chunking.maxOverlapSizeInChars}") int maxOverlapSizeInChars) {
		return DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars);
	}
}
