package com.knowgauge.core.service.retrieving;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.port.embedding.EmbeddingService;
import com.knowgauge.core.port.vectorstore.VectorStore;

/** FOCUSED coverage. Vectors are 2D, so each chunk sits at an exact cosine distance from the query (1, 0). */
class RetrievingServiceImplTest {

	private static final long TENANT = 1L;
	private static final List<Long> DOCS = List.of(10L, 20L);
	private static final float[] QUERY = { 1f, 0f };

	private VectorStore vectorStore;
	private EmbeddingService embeddingService;
	private FocusedRetrievalProperties properties;
	private RetrievingServiceImpl service;

	@BeforeEach
	void setUp() {
		vectorStore = mock(VectorStore.class);
		embeddingService = mock(EmbeddingService.class);
		when(embeddingService.modelName()).thenReturn("text-embedding-3-small");
		when(embeddingService.embed("on-call rotation")).thenReturn(QUERY);
		properties = new FocusedRetrievalProperties(); // max distance 0.65, gap 0.20
		service = new RetrievingServiceImpl(vectorStore, embeddingService, properties);
	}

	@Test
	void keepsOnlyChunksCloseToTheBestMatch() {
		givenNearest(chunk(1, 0.30), chunk(2, 0.40), chunk(3, 0.48), chunk(4, 0.53), chunk(5, 0.62));

		assertThat(focused(40)).extracting(ChunkEmbedding::getChunkId).containsExactly(1L, 2L, 3L);
	}

	@Test
	void neverUsesChunksBeyondTheMaximumDistance() {
		givenNearest(chunk(1, 0.60), chunk(2, 0.64), chunk(3, 0.70));

		assertThat(focused(40)).extracting(ChunkEmbedding::getChunkId).containsExactly(1L, 2L);
	}

	@Test
	void anUncoveredFocusYieldsNoChunks() {
		givenNearest(chunk(1, 0.75), chunk(2, 0.80));

		assertThat(focused(40)).isEmpty();
	}

	@Test
	void usesAtMostTheRequestedLimit() {
		givenNearest(chunk(1, 0.30), chunk(2, 0.31), chunk(3, 0.32));

		assertThat(focused(2)).extracting(ChunkEmbedding::getChunkId).containsExactly(1L, 2L);
	}

	@Test
	void takesIdenticalContentOnce() {
		ChunkEmbedding duplicate = chunk(2, 0.31);
		duplicate.setChunkChecksum("c1");
		givenNearest(chunk(1, 0.30), duplicate, chunk(3, 0.32));

		assertThat(focused(40)).extracting(ChunkEmbedding::getChunkId).containsExactly(1L, 3L);
	}

	@Test
	void asksTheVectorStoreForTwiceTheLimit() {
		givenNearest();

		focused(40);

		verify(vectorStore).findNearest(List.of(TENANT), DOCS, QUERY, "text-embedding-3-small", 85);
		verify(vectorStore, never()).findCandidates(any(), any(), anyInt(), anyString(), anyBoolean());
	}

	@Test
	void needsAFocusText() {
		assertThatThrownBy(() -> service.retrieveTop(List.of(TENANT), DOCS, 5, TestCoverageMode.FOCUSED, " ", false))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("focus text");
		verify(embeddingService, never()).embed(anyString());
	}

	@Test
	void cosineDistanceMatchesPgvector() {
		assertThat(RetrievingServiceImpl.cosineDistance(QUERY, QUERY)).isCloseTo(0.0, within(1e-6));
		assertThat(RetrievingServiceImpl.cosineDistance(QUERY, new float[] { 0f, 1f })).isCloseTo(1.0, within(1e-6));
		assertThat(RetrievingServiceImpl.cosineDistance(QUERY, vector(0.4))).isCloseTo(0.4, within(1e-6));
	}

	private List<ChunkEmbedding> focused(int limit) {
		return service.retrieveTop(List.of(TENANT), DOCS, limit, TestCoverageMode.FOCUSED, "on-call rotation", false);
	}

	private void givenNearest(ChunkEmbedding... chunks) {
		when(vectorStore.findNearest(eq(List.of(TENANT)), eq(DOCS), eq(QUERY), eq("text-embedding-3-small"), anyInt()))
				.thenReturn(List.of(chunks));
	}

	private static ChunkEmbedding chunk(long chunkId, double distance) {
		return ChunkEmbedding.builder().chunkId(chunkId).documentId(10L).chunkChecksum("c" + chunkId)
				.embedding(vector(distance)).build();
	}

	/** A unit vector at the given cosine distance from (1, 0). */
	private static float[] vector(double distance) {
		double cos = 1 - distance;
		return new float[] { (float) cos, (float) Math.sqrt(1 - cos * cos) };
	}
}
