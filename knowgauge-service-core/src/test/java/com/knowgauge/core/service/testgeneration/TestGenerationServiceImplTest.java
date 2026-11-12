package com.knowgauge.core.service.testgeneration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.InsufficientContextException;
import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.retrieving.RetrievingService;
import com.knowgauge.core.service.testgeneration.validation.TestDraftValidator;
import com.knowgauge.core.support.QuestionFixtures;

class TestGenerationServiceImplTest {

	private static final long TENANT = QuestionFixtures.TENANT;
	private static final long TEST_ID = QuestionFixtures.TEST_ID;

	private RetrievingService retrievingService;
	private DocumentChunkRepository chunkRepository;
	private QuestionBatchGenerator batchGenerator;
	private TestGenerationTransactionalServiceImpl tx;
	private TestGenerationServiceImpl service;

	@BeforeEach
	void setUp() {
		retrievingService = mock(RetrievingService.class);
		chunkRepository = mock(DocumentChunkRepository.class);
		batchGenerator = mock(QuestionBatchGenerator.class);
		tx = mock(TestGenerationTransactionalServiceImpl.class);
		TestDraftValidator draftValidator = mock(TestDraftValidator.class);
		ExecutionContext ctx = mock(ExecutionContext.class);
		when(ctx.tenantId()).thenReturn(TENANT);
		when(ctx.userId()).thenReturn(OptionalLong.of(42L));

		TestGenerationDefaultsProperties defaults = new TestGenerationDefaultsProperties();
		defaults.setQuestionCount(10);
		defaults.setQuestionGenerationBatchSize(5);
		defaults.setChunksPerQuestion(4);
		defaults.setMinChunksPerTest(20);
		defaults.setMinChunksPerQuestion(1);
		defaults.setMaxConsecutiveZeroProgressBatches(1);
		defaults.setCoverageMode(TestCoverageMode.BALANCED_PER_DOC_CHUNKS);
		defaults.setAnswerCardinality(AnswerCardinality.SINGLE_CORRECT);
		defaults.setAvoidRepeats(false);
		defaults.setPromptTemplateId("mcq-default");
		defaults.setGenerationModel("gpt-4.1");

		when(draftValidator.validateAndExpandTopicsAndDocuments(eq(TENANT), any()))
				.thenAnswer(inv -> inv.getArgument(1));
		when(tx.persistTest(eq(TENANT), any(), any())).thenAnswer(inv -> {
			com.knowgauge.core.model.Test t = inv.getArgument(1);
			t.setId(TEST_ID);
			return t;
		});

		service = new TestGenerationServiceImpl(retrievingService, batchGenerator, draftValidator, tx,
				mock(TestRepository.class), mock(TestQuestionRepository.class), chunkRepository, ctx, defaults);
	}

	private static com.knowgauge.core.model.Test draft(int questionCount) {
		return com.knowgauge.core.model.Test.builder().documentIds(List.of(3L, 4L)).questionCount(questionCount)
				.build();
	}

	/** Retrieval returns chunk ids 101..(100+count). */
	private void givenChunks(int count) {
		List<ChunkEmbedding> embeddings = new ArrayList<>();
		List<DocumentChunk> loaded = new ArrayList<>();
		for (Long id : LongStream.rangeClosed(101, 100 + count).boxed().toList()) {
			embeddings.add(ChunkEmbedding.builder().chunkId(id).documentId(3L).build());
			loaded.add(DocumentChunk.builder().id(id).chunkText("chunk " + id).build());
		}
		when(retrievingService.retrieveTop(eq(TENANT), anyList(), anyInt(), any(), anyBoolean()))
				.thenReturn(embeddings);
		when(chunkRepository.findByTenantIdAndIdIn(eq(TENANT), anyList())).thenReturn(loaded);
	}

	@Test
	void fewerChunksThanQuestionsFailsBeforeAnyLlmCall() {
		givenChunks(5);

		assertThatThrownBy(() -> service.generate(draft(10))).isInstanceOf(InsufficientContextException.class)
				.hasMessageContaining("5 chunks").hasMessageContaining("at least 10");

		verify(tx).markTestFailed(eq(TENANT), eq(TEST_ID), anyString());
		verify(batchGenerator, never()).generateBatch(any());
		verify(tx, never()).setUsedChunks(anyLong(), anyLong(), anyList());
	}

	@Test
	void noChunksIsInsufficientContext() {
		givenChunks(0);

		assertThatThrownBy(() -> service.generate(draft(10))).isInstanceOf(InsufficientContextException.class)
				.hasMessageContaining("0 chunks");
		verify(batchGenerator, never()).generateBatch(any());
	}
}
