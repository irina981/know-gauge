package com.knowgauge.langchain.service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.knowgauge.core.port.ingestion.EmbeddingService;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

@Service
public class OpenAiEmbeddingServiceImpl implements EmbeddingService {

	private final EmbeddingModel model;
	private final String modelName;
	private final int expectedDimension;

	public OpenAiEmbeddingServiceImpl(@Value("${kg.embedding.openai.api-key}") String apiKey,
			@Value("${kg.embedding.openai.model:text-embedding-3-small}") String modelName,
			@Value("${kg.embedding.openai.base-url:}") String baseUrl,
			@Value("${kg.embedding.dimension}") int expectedDimension) {
		this.modelName = modelName;

		OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder().apiKey(apiKey)
				.modelName(modelName).timeout(Duration.ofSeconds(30));

		// baseUrl is optional (useful for proxies / OpenAI-compatible providers)
		if (baseUrl != null && !baseUrl.isBlank()) {
			builder.baseUrl(baseUrl);
		}

		this.model = builder.build();
		this.expectedDimension = expectedDimension;
	}

	@Override
	public float[] embed(String text) {
		String safe = normalize(text);

		Embedding embedding = model.embed(safe).content(); // Response<Embedding> -> Embedding
		float[] vector = embedding.vector();

		if (vector == null || vector.length == 0) {
			throw new IllegalStateException("OpenAI returned empty embedding vector");
		}

		validate(vector);
		
		return vector;
	}

	@Override
	public List<float[]> embed(List<String> texts) {
		if (texts == null || texts.isEmpty()) {
			return List.of();
		}

		List<TextSegment> segments = texts.stream().map(this::normalize).map(TextSegment::from).toList();

		List<Embedding> embeddings = model.embedAll(segments).content(); // Response<List<Embedding>>
		if (embeddings == null || embeddings.size() != segments.size()) {
			throw new IllegalStateException("Unexpected embedding response size");
		}

		return embeddings.stream().map(Embedding::vector).toList();
	}

	@Override
	public String modelName() {
		return modelName;
	}

	private String normalize(String text) {
		String t = Objects.requireNonNull(text, "Text must not be null").trim();
		if (t.isEmpty()) {
			// Decide policy: either reject or embed empty (OpenAI may reject empty)
			throw new IllegalArgumentException("Text must not be blank");
		}
		return t;
	}

	private void validate(float[] vector) {
		if (vector == null) {
			throw new IllegalStateException("Embedding vector is null");
		}

		if (vector.length != expectedDimension) {
			throw new IllegalStateException("Embedding dimension mismatch. Expected " + expectedDimension + " but got "
					+ vector.length + " for model " + modelName);
		}
	}
}
