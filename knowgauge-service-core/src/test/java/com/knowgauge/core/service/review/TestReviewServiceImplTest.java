package com.knowgauge.core.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.QuestionValidationException;
import com.knowgauge.core.exception.RegenerationFailedException;
import com.knowgauge.core.exception.ReviewConflictException;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.GenerationRunPurpose;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.model.enums.VersionOrigin;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.repository.GenerationRunRepository;
import com.knowgauge.core.port.repository.TestEventRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.review.ManualQuestionDraft.ManualOption;
import com.knowgauge.core.service.review.TestReviewTransactionalServiceImpl.VersionChange;
import com.knowgauge.core.service.testgeneration.QuestionBatchGenerator;
import com.knowgauge.core.service.testgeneration.QuestionBatchGenerator.BatchRequest;
import com.knowgauge.core.service.testgeneration.validation.TestQuestionValidator;
import com.knowgauge.core.support.QuestionFixtures;

class TestReviewServiceImplTest {

	private static final long QUESTION_ID = 7L;
	private static final long VERSION_ID = 70L;
	private static final long USER = 42L;

	private TestRepository testRepository;
	private TestQuestionRepository questionRepository;
	private DocumentChunkRepository chunkRepository;
	private QuestionBatchGenerator batchGenerator;
	private TestReviewTransactionalServiceImpl tx;
	private TestReviewServiceImpl service;

	private TestQuestion question;

	@BeforeEach
	void setUp() {
		testRepository = mock(TestRepository.class);
		questionRepository = mock(TestQuestionRepository.class);
		chunkRepository = mock(DocumentChunkRepository.class);
		batchGenerator = mock(QuestionBatchGenerator.class);
		tx = mock(TestReviewTransactionalServiceImpl.class);
		ExecutionContext ctx = mock(ExecutionContext.class);
		when(ctx.tenantId()).thenReturn(QuestionFixtures.TENANT);
		when(ctx.userId()).thenReturn(OptionalLong.of(USER));

		service = new TestReviewServiceImpl(testRepository, questionRepository, mock(TestEventRepository.class),
				mock(GenerationRunRepository.class), chunkRepository, batchGenerator, new TestQuestionValidator(), tx,
				ctx);

		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.getOption(OptionLetter.B).flag("ArrayDeque", "HashMap may keep order for small maps");
		question = QuestionFixtures.persisted(QUESTION_ID, 2, VERSION_ID, v);

		givenTest(TestStatus.REQUIRES_REVIEW, AnswerCardinality.SINGLE_CORRECT);
		when(questionRepository.findByTenantIdAndId(QuestionFixtures.TENANT, QUESTION_ID))
				.thenReturn(Optional.of(question));
		when(questionRepository.findByTenantIdAndTestId(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID))
				.thenReturn(List.of(question));
		when(tx.applyNewVersion(anyLong(), any(), anyLong(), any(), any())).thenReturn(question);
	}

	private void givenTest(TestStatus status, AnswerCardinality cardinality) {
		when(testRepository.findByTenantIdAndId(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID))
				.thenReturn(Optional.of(QuestionFixtures.test(cardinality, status)));
	}

	// ---------------- accept fix ----------------

	@Test
	void acceptFixAppliesSuggestionsAsNewVersionByCurrentUser() {
		service.acceptFix(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, "looks right");

		ArgumentCaptor<TestQuestionVersion> draft = ArgumentCaptor.forClass(TestQuestionVersion.class);
		ArgumentCaptor<VersionChange> change = ArgumentCaptor.forClass(VersionChange.class);
		verify(tx).applyNewVersion(eq(QuestionFixtures.TENANT), eq(question), eq(VERSION_ID), draft.capture(),
				change.capture());

		assertThat(draft.getValue().getOption(OptionLetter.B).getText()).isEqualTo("ArrayDeque");
		assertThat(draft.getValue().hasFlags()).isFalse();
		assertThat(draft.getValue().getCorrectOptions()).containsExactly(OptionLetter.A);
		assertThat(change.getValue().origin()).isEqualTo(VersionOrigin.FIX_ACCEPTED);
		assertThat(change.getValue().eventType()).isEqualTo(TestEventType.FIX_ACCEPTED);
		assertThat(change.getValue().actorUserId()).isEqualTo(USER);
		assertThat(change.getValue().comment()).isEqualTo("looks right");
	}

	@Test
	void acceptFixWithoutFlagsIsAConflict() {
		TestQuestion clean = QuestionFixtures.persisted(QUESTION_ID, 2, VERSION_ID, QuestionFixtures.defaultVersion());
		when(questionRepository.findByTenantIdAndId(QuestionFixtures.TENANT, QUESTION_ID))
				.thenReturn(Optional.of(clean));

		assertThatThrownBy(() -> service.acceptFix(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("no flagged options");
	}

	// ---------------- dismiss flags (keep as is) ----------------

	@Test
	void dismissFlagsKeepsTheContentWithoutFlagsAsNewVersion() {
		question.getCurrentVersion().setGenerationRunId(5L);
		question.getCurrentVersion().setVerificationRunId(6L);

		service.dismissFlags(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, "  HashMap is a fine distractor  ");

		ArgumentCaptor<TestQuestionVersion> draft = ArgumentCaptor.forClass(TestQuestionVersion.class);
		ArgumentCaptor<VersionChange> change = ArgumentCaptor.forClass(VersionChange.class);
		verify(tx).applyNewVersion(eq(QuestionFixtures.TENANT), eq(question), eq(VERSION_ID), draft.capture(),
				change.capture());

		TestQuestionVersion kept = draft.getValue();
		assertThat(kept.hasFlags()).isFalse();
		assertThat(kept.getOption(OptionLetter.B).getText()).isEqualTo("HashMap");
		assertThat(kept.getOption(OptionLetter.B).getSuggestedText()).isNull();
		assertThat(kept.getQuestionText()).isEqualTo(question.getCurrentVersion().getQuestionText());
		assertThat(kept.getCorrectOptions()).containsExactly(OptionLetter.A);
		assertThat(kept.getGenerationRunId()).isEqualTo(5L);
		assertThat(kept.getVerificationRunId()).isEqualTo(6L);
		assertThat(change.getValue().origin()).isEqualTo(VersionOrigin.FLAGS_DISMISSED);
		assertThat(change.getValue().eventType()).isEqualTo(TestEventType.FLAGS_DISMISSED);
		assertThat(change.getValue().actorUserId()).isEqualTo(USER);
		assertThat(change.getValue().comment()).isEqualTo("HashMap is a fine distractor");
		@SuppressWarnings("unchecked")
		Map<String, Map<String, Object>> dismissed = (Map<String, Map<String, Object>>) change.getValue().payload()
				.get("dismissedFlags");
		assertThat(dismissed).containsOnlyKeys("B");
		assertThat(dismissed.get("B")).containsEntry("text", "HashMap").containsEntry("suggestedText", "ArrayDeque");
		// The version the question was at keeps its flags (versions are immutable)
		assertThat(question.getCurrentVersion().hasFlags()).isTrue();
	}

	@Test
	void dismissFlagsRequiresAComment() {
		assertThatThrownBy(() -> service.dismissFlags(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, "  "))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("comment is required");
		verify(tx, never()).applyNewVersion(anyLong(), any(), anyLong(), any(), any());
	}

	@Test
	void dismissFlagsWithoutFlagsIsAConflict() {
		TestQuestion clean = QuestionFixtures.persisted(QUESTION_ID, 2, VERSION_ID, QuestionFixtures.defaultVersion());
		when(questionRepository.findByTenantIdAndId(QuestionFixtures.TENANT, QUESTION_ID))
				.thenReturn(Optional.of(clean));

		assertThatThrownBy(() -> service.dismissFlags(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, "ok"))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("no flagged options");
	}

	@Test
	void staleExpectedVersionIsAConflict() {
		assertThatThrownBy(() -> service.acceptFix(QuestionFixtures.TEST_ID, QUESTION_ID, 69L, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("Reload");
		verify(tx, never()).applyNewVersion(anyLong(), any(), anyLong(), any(), any());
	}

	@Test
	void testThatIsStillGeneratingCannotBeReviewed() {
		givenTest(TestStatus.CREATED, AnswerCardinality.SINGLE_CORRECT);

		assertThatThrownBy(() -> service.acceptFix(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("CREATED");
	}

	@Test
	void publishedTestCannotBeRegeneratedAndNoLlmCallIsMade() {
		givenTest(TestStatus.PUBLISHED, AnswerCardinality.SINGLE_CORRECT);

		assertThatThrownBy(() -> service.regenerate(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("PUBLISHED");
		verify(batchGenerator, never()).generateBatch(any());
		verify(tx, never()).applyNewVersion(anyLong(), any(), anyLong(), any(), any());
	}

	@Test
	void publishRunsAsTheCurrentUser() {
		service.publish(QuestionFixtures.TEST_ID, "approved");

		verify(tx).publish(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID, USER, "approved");
	}

	// ---------------- manual replace ----------------

	private ManualQuestionDraft manual(String text, Set<OptionLetter> correct, String... options) {
		EnumMap<OptionLetter, ManualOption> map = new EnumMap<>(OptionLetter.class);
		OptionLetter[] letters = OptionLetter.values();
		for (int i = 0; i < options.length; i++) {
			map.put(letters[i], new ManualOption(options[i], correct.contains(letters[i])));
		}
		return new ManualQuestionDraft(VERSION_ID, text, map, null, "rewritten by hand");
	}

	@Test
	void manualReplaceCreatesUnflaggedManualVersion() {
		service.manualReplace(QuestionFixtures.TEST_ID, QUESTION_ID, manual("Which map keeps keys sorted?",
				Set.of(OptionLetter.C), "HashMap", "LinkedHashMap", "TreeMap", "WeakHashMap"));

		ArgumentCaptor<TestQuestionVersion> draft = ArgumentCaptor.forClass(TestQuestionVersion.class);
		ArgumentCaptor<VersionChange> change = ArgumentCaptor.forClass(VersionChange.class);
		verify(tx).applyNewVersion(eq(QuestionFixtures.TENANT), eq(question), eq(VERSION_ID), draft.capture(),
				change.capture());
		assertThat(draft.getValue().getCorrectOptions()).containsExactly(OptionLetter.C);
		assertThat(draft.getValue().hasFlags()).isFalse();
		assertThat(draft.getValue().getGenerationRunId()).isNull();
		assertThat(draft.getValue().getSourceChunkIds()).containsExactly(11L, 12L);
		assertThat(change.getValue().origin()).isEqualTo(VersionOrigin.MANUAL);
		assertThat(change.getValue().eventType()).isEqualTo(TestEventType.QUESTION_MANUALLY_REPLACED);
		assertThat(change.getValue().actorUserId()).isEqualTo(USER);
	}

	@Test
	void invalidManualQuestionIsRejectedWithReasonsAndNothingIsWritten() {
		assertThatThrownBy(() -> service.manualReplace(QuestionFixtures.TEST_ID, QUESTION_ID,
				manual("Which two are maps?", Set.of(OptionLetter.A, OptionLetter.B), "HashMap", "TreeMap",
						"ArrayList", "LinkedList")))
				.isInstanceOf(QuestionValidationException.class)
				.satisfies(ex -> assertThat(((QuestionValidationException) ex).getReasons()).hasSizeGreaterThan(1));
		verify(tx, never()).applyNewVersion(anyLong(), any(), anyLong(), any(), any());
	}

	@Test
	void manualQuestionMissingAnOptionIsRejected() {
		assertThatThrownBy(() -> service.manualReplace(QuestionFixtures.TEST_ID, QUESTION_ID,
				manual("Which map keeps keys sorted?", Set.of(OptionLetter.C), "HashMap", "LinkedHashMap", "TreeMap")))
				.isInstanceOf(QuestionValidationException.class).hasMessageContaining("option D is missing");
	}

	// ---------------- regenerate ----------------

	@Test
	void regenerateCreatesRegeneratedVersionFromOriginalContext() {
		when(testRepository.findUsedChunkIds(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID))
				.thenReturn(List.of(11L, 12L));
		when(chunkRepository.findByTenantIdAndIdIn(eq(QuestionFixtures.TENANT), any()))
				.thenReturn(List.of(new DocumentChunk()));
		TestQuestionVersion newContent = QuestionFixtures.version("Which interface does TreeMap implement?",
				Set.of(OptionLetter.D), "List", "Queue", "Deque", "SortedMap");
		newContent.setGenerationRunId(501L);
		when(batchGenerator.generateBatch(any())).thenReturn(List.of(QuestionFixtures.draft(newContent)));

		service.regenerate(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, null);

		ArgumentCaptor<BatchRequest> request = ArgumentCaptor.forClass(BatchRequest.class);
		verify(batchGenerator).generateBatch(request.capture());
		assertThat(request.getValue().batchSize()).isEqualTo(1);
		assertThat(request.getValue().purpose()).isEqualTo(GenerationRunPurpose.REGENERATION);
		assertThat(request.getValue().questionId()).isEqualTo(QUESTION_ID);
		assertThat(request.getValue().chunkIds()).containsExactly(11L, 12L);
		assertThat(request.getValue().avoidQuestionTexts()).contains(question.getQuestionText());

		ArgumentCaptor<VersionChange> change = ArgumentCaptor.forClass(VersionChange.class);
		verify(tx).applyNewVersion(eq(QuestionFixtures.TENANT), eq(question), eq(VERSION_ID), eq(newContent),
				change.capture());
		assertThat(change.getValue().origin()).isEqualTo(VersionOrigin.REGENERATED);
		assertThat(change.getValue().payload()).containsEntry("generationRunId", 501L);
	}

	@Test
	void failedRegenerationIsRecordedAndChangesNothing() {
		when(testRepository.findUsedChunkIds(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID))
				.thenReturn(List.of(11L));
		when(chunkRepository.findByTenantIdAndIdIn(eq(QuestionFixtures.TENANT), any()))
				.thenReturn(List.of(new DocumentChunk()));
		when(batchGenerator.generateBatch(any())).thenReturn(List.of());

		assertThatThrownBy(() -> service.regenerate(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, "try again"))
				.isInstanceOf(RegenerationFailedException.class);
		verify(tx).recordQuestionEvent(eq(question), eq(TestEventType.REGENERATION_FAILED), eq(USER),
				eq("try again"), any(Map.class));
		verify(tx, never()).applyNewVersion(anyLong(), any(), anyLong(), any(), any());
	}

	@Test
	void duplicateRegenerationIsRejected() {
		when(testRepository.findUsedChunkIds(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID))
				.thenReturn(List.of(11L));
		when(chunkRepository.findByTenantIdAndIdIn(eq(QuestionFixtures.TENANT), any()))
				.thenReturn(List.of(new DocumentChunk()));
		when(batchGenerator.generateBatch(any()))
				.thenReturn(List.of(QuestionFixtures.draft(QuestionFixtures.defaultVersion())));

		assertThatThrownBy(() -> service.regenerate(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, null))
				.isInstanceOf(RegenerationFailedException.class).hasMessageContaining("duplicate");
		verify(tx).recordQuestionEvent(eq(question), eq(TestEventType.REGENERATION_FAILED), eq(USER), any(),
				any(Map.class));
	}

	@Test
	void regenerationWithoutContextFails() {
		when(testRepository.findUsedChunkIds(QuestionFixtures.TENANT, QuestionFixtures.TEST_ID)).thenReturn(List.of());

		assertThatThrownBy(() -> service.regenerate(QuestionFixtures.TEST_ID, QUESTION_ID, VERSION_ID, null))
				.isInstanceOf(RegenerationFailedException.class).hasMessageContaining("source chunks");
		verify(batchGenerator, never()).generateBatch(any());
		verify(tx).recordQuestionEvent(any(), eq(TestEventType.REGENERATION_FAILED), anyLong(), any(), any());
		verify(tx, never()).applyNewVersion(anyLong(), any(), anyLong(), any(), any());
	}
}
