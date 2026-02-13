package com.knowgauge.infra.embedding.openai.service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.EmbeddingAuthException;
import com.knowgauge.core.exception.EmbeddingBadRequestException;
import com.knowgauge.core.exception.EmbeddingRateLimitedException;
import com.knowgauge.core.exception.EmbeddingUnavailableException;
import com.knowgauge.core.exception.EmbeddingUnexpectedException;
import com.knowgauge.core.port.embedding.EmbeddingService;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class OpenAiEmbeddingServiceImpl implements EmbeddingService {

	private final EmbeddingModel model;
	private final String modelName;
	private final int expectedDimension;

	public OpenAiEmbeddingServiceImpl(@Value("${kg.embedding.openai.api-key}") String apiKey,
			@Value("${kg.embedding.openai.model:text-embedding-3-small}") String modelName,
			@Value("${kg.embedding.openai.base-url:}") String baseUrl,
			@Value("${kg.embedding.dimension}") int expectedDimension,
			@Value("${kg.embedding.openai.timeout}") int modelTimeout) {
		this.modelName = modelName;

		OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder().apiKey(apiKey)
				.modelName(modelName).timeout(Duration.ofSeconds(modelTimeout));

		// baseUrl is optional (useful for proxies / OpenAI-compatible providers)
		if (baseUrl != null && !baseUrl.isBlank()) {
			builder.baseUrl(baseUrl);
		}

		this.model = builder.build();
		this.expectedDimension = expectedDimension;
	}

	@Override
	@Retry(name = "openaiEmbedding")
	@Bulkhead(name = "openaiEmbedding", type = Bulkhead.Type.SEMAPHORE)
	@CircuitBreaker(name = "openaiEmbedding", fallbackMethod = "embedFallback")
	public float[] embed(String text) {
		String safe = normalize(text);

		try {
			Embedding embedding = model.embed(safe).content();
			float[] vector = embedding.vector();

			if (vector == null || vector.length == 0) {
				throw new EmbeddingUnexpectedException("Provider returned empty embedding vector", null);
			}
			validate(vector);
			return vector;

		} catch (Exception e) {
			throw translateEmbeddingException(e);
		}
	}

	@Override
	@Retry(name = "openaiEmbeddingBatch")
	@Bulkhead(name = "openaiEmbeddingBatch", type = Bulkhead.Type.SEMAPHORE)
	@CircuitBreaker(name = "openaiEmbeddingBatch", fallbackMethod = "embedBatchFallback")
	public List<float[]> embed(List<String> texts) {
		if (texts == null || texts.isEmpty())
			return List.of();

		List<TextSegment> segments = texts.stream().map(this::normalize).map(TextSegment::from).toList();

		try {
			List<Embedding> embeddings = model.embedAll(segments).content();
			if (embeddings == null || embeddings.size() != segments.size()) {
				throw new EmbeddingUnexpectedException("Unexpected embedding response size", null);
			}

			List<float[]> vectors = embeddings.stream().map(Embedding::vector).toList();
			vectors.forEach(this::validate);
			return vectors;

		} catch (Exception e) {
			throw translateEmbeddingException(e);
		}
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

	private float[] embedFallback(String text, Throwable t) {
		// Decide policy: either rethrow or return sentinel value.
		// For embeddings, returning fake vectors is usually dangerous -> rethrow a
		// domain exception.
		throw new IllegalStateException("Embedding failed (fallback). " + t.getMessage(), t);
	}

	private List<float[]> embedBatchFallback(List<String> texts, Throwable t) {
		throw new IllegalStateException("Batch embedding failed (fallback). " + t.getMessage(), t);
	}

	private RuntimeException translateEmbeddingException(Exception e) {

		// Network/IO → transient
		if (e instanceof SocketTimeoutException || e instanceof IOException) {
			return new EmbeddingUnavailableException("Embedding call failed due to IO/timeout", e);
		}

		// LangChain4j HTTP wrapper
		if (e instanceof HttpException he) {
			int code = he.statusCode(); // if your version uses a different accessor, adjust

			return switch (code) {
			case 400, 404, 422 -> new EmbeddingBadRequestException("Embedding request rejected (" + code + ")", e);
			case 401, 403 -> new EmbeddingAuthException("Embedding auth/permission error (" + code + ")", e);
			case 408 -> new EmbeddingUnavailableException("Embedding request timed out (" + code + ")", e);
			case 429 -> new EmbeddingRateLimitedException("Embedding rate limited (" + code + ")", e);
			default -> {
				// 5xx → transient
				if (code >= 500 && code <= 599) {
					yield new EmbeddingUnavailableException("Embedding provider unavailable (" + code + ")", e);
				}
				yield new EmbeddingUnexpectedException("Embedding HTTP error (" + code + ")", e);
			}
			};
		}

		// Anything else
		return new EmbeddingUnexpectedException("Unexpected embedding failure", e);
	}
}
