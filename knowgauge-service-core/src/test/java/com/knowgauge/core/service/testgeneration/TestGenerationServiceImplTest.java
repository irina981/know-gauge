package com.knowgauge.core.service.testgeneration;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.InsufficientContextException;
import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.retrieving.RetrievingService;
import com.knowgauge.core.service.testgeneration.QuestionBatchGenerator.BatchRequest;
import com.knowgauge.core.service.testgeneration.TestGenerationServiceImpl.ChunksContext;
import com.knowgauge.core.service.testgeneration.validation.TestDraftValidator;
import com.knowgauge.core.service.testgeneration.validation.TestQuestionValidator;
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
		when(tx.persistGeneratedQuestions(eq(TENANT), eq(TEST_ID), anyList(), anyList()))
				.thenAnswer(inv -> inv.getArgument(2));
		when(tx.markTestGenerated(TENANT, TEST_ID))
				.thenReturn(QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT, TestStatus.GENERATED));

		service = new TestGenerationServiceImpl(retrievingService, batchGenerator, draftValidator, tx,
				mock(TestRepository.class), mock(TestQuestionRepository.class), chunkRepository, ctx, defaults,
				new TestQuestionValidator());
	}

	private static com.knowgauge.core.model.Test draft(int questionCount) {
		return com.knowgauge.core.model.Test.builder().documentIds(List.of(3L, 4L)).questionCount(questionCount)
				.build();
	}

	/** Retrieval returns chunk ids 101..(100+count); the repository loads them in reverse order. */
	private List<Long> givenChunks(int count) {
		List<Long> ids = LongStream.rangeClosed(101, 100 + count).boxed().toList();
		List<ChunkEmbedding> embeddings = new ArrayList<>();
		List<DocumentChunk> loaded = new ArrayList<>();
		for (Long id : ids) {
			embeddings.add(ChunkEmbedding.builder().chunkId(id).documentId(3L).build());
			loaded.add(DocumentChunk.builder().id(id).chunkText("chunk " + id).build());
		}
		when(retrievingService.retrieveTop(eq(TENANT), anyList(), anyInt(), any(), anyBoolean()))
				.thenReturn(embeddings);
		java.util.Collections.reverse(loaded);
		when(chunkRepository.findByTenantIdAndIdIn(eq(TENANT), anyList())).thenReturn(loaded);
		return ids;
	}

	/** Each batch returns {@code size} questions whose texts come from {@code text(batchIndex, i)}. */
	private void givenBatches(Function<int[], String> text) {
		when(batchGenerator.generateBatch(any())).thenAnswer(inv -> {
			BatchRequest req = inv.getArgument(0);
			List<TestQuestion> out = new ArrayList<>();
			for (int i = 0; i < req.batchSize(); i++) {
				String q = text.apply(new int[] { req.batchIndex(), i });
				TestQuestion question = QuestionFixtures.draft(QuestionFixtures.version(q, Set.of(OptionLetter.A),
						q + " a", q + " b", q + " c", q + " d"));
				question.setQuestionIndex(req.startIndex() + i);
				out.add(question);
			}
			return out;
		});
	}

	@Test
	void eachBatchGetsItsOwnChunkSliceAndTheQuestionsToAvoid() {
		List<Long> ids = givenChunks(15);
		givenBatches(bi -> "Question " + bi[0] + "." + bi[1]);

		service.generate(draft(10));

		ArgumentCaptor<BatchRequest> req = ArgumentCaptor.forClass(BatchRequest.class);
		verify(batchGenerator, times(2)).generateBatch(req.capture());
		BatchRequest first = req.getAllValues().get(0);
		BatchRequest second = req.getAllValues().get(1);

		assertThat(first.chunkIds()).hasSize(7).doesNotContainAnyElementsOf(second.chunkIds());
		List<Long> union = new ArrayList<>(first.chunkIds());
		union.addAll(second.chunkIds());
		assertThat(union).isEqualTo(ids);
		assertThat(first.chunks()).extracting(DocumentChunk::getId).isEqualTo(first.chunkIds());
		assertThat(second.chunks()).extracting(DocumentChunk::getId).isEqualTo(second.chunkIds());

		assertThat(first.avoidQuestionTexts()).isEmpty();
		assertThat(second.avoidQuestionTexts()).containsExactly("Question 1.0", "Question 1.1", "Question 1.2",
				"Question 1.3", "Question 1.4");
	}

	@Test
	void duplicatesOfEarlierQuestionsAreDroppedAndIndexesStaySequential() {
		givenChunks(15);
		// Batch 2 repeats batch 1's first question; the shortfall is filled by batch 3
		givenBatches(bi -> bi[0] == 2 && bi[1] == 0 ? "Question 1.0" : "Question " + bi[0] + "." + bi[1]);

		service.generate(draft(10));

		ArgumentCaptor<List<TestQuestion>> saved = ArgumentCaptor.captor();
		verify(tx).persistGeneratedQuestions(eq(TENANT), eq(TEST_ID), saved.capture(), anyList());
		List<TestQuestion> questions = saved.getValue();
		assertThat(questions).extracting(TestQuestion::getQuestionIndex).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8,
				9);
		assertThat(new HashSet<>(questions.stream().map(TestQuestion::getQuestionText).toList())).hasSize(10);
		verify(batchGenerator, times(3)).generateBatch(any());
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

	@Test
	void slicesNeverOutnumberChunks() {
		List<DocumentChunk> chunks = List.of(DocumentChunk.builder().id(1L).build(),
				DocumentChunk.builder().id(2L).build());
		List<ChunksContext> slices = TestGenerationServiceImpl.sliceChunks(new ChunksContext(chunks, List.of(1L, 2L)),
				4);

		assertThat(slices).hasSize(2);
		assertThat(slices).extracting(ChunksContext::chunkIds).containsExactly(List.of(1L), List.of(2L));
	}
}
