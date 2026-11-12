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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.exception.ReviewConflictException;
import com.knowgauge.core.model.TestEvent;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.QuestionStatus;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.model.enums.VersionOrigin;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.core.port.repository.TestEventRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.history.TestHistoryRecorder;
import com.knowgauge.core.service.review.TestReviewTransactionalServiceImpl.VersionChange;
import com.knowgauge.core.support.QuestionFixtures;

class TestReviewTransactionalServiceImplTest {

	private static final long TENANT = QuestionFixtures.TENANT;
	private static final long TEST_ID = QuestionFixtures.TEST_ID;

	private TestRepository testRepository;
	private TestQuestionRepository questionRepository;
	private TestEventRepository eventRepository;
	private AttemptRepository attemptRepository;
	private TestHistoryRecorder history;
	private TestReviewTransactionalServiceImpl tx;

	private TestQuestion flaggedQuestion;

	@BeforeEach
	void setUp() {
		testRepository = mock(TestRepository.class);
		questionRepository = mock(TestQuestionRepository.class);
		history = mock(TestHistoryRecorder.class);
		eventRepository = mock(TestEventRepository.class);
		attemptRepository = mock(AttemptRepository.class);
		tx = new TestReviewTransactionalServiceImpl(testRepository, questionRepository, eventRepository,
				attemptRepository, history);

		TestQuestionVersion v = QuestionFixtures.defaultVersion();
		v.getOption(OptionLetter.B).flag("ArrayDeque", "r");
		flaggedQuestion = QuestionFixtures.persisted(7L, 0, 70L, v);

		when(questionRepository.findMaxVersionNo(TENANT, 7L)).thenReturn(1);
		when(questionRepository.insertVersion(any())).thenAnswer(inv -> {
			TestQuestionVersion saved = inv.getArgument(0);
			saved.setId(71L);
			return saved;
		});
		when(questionRepository.findByTenantIdAndId(TENANT, 7L)).thenReturn(Optional.of(flaggedQuestion));
		when(testRepository.findByTenantIdAndIdForUpdate(TENANT, TEST_ID)).thenReturn(
				Optional.of(QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT, TestStatus.REQUIRES_REVIEW)));
	}

	private VersionChange fix() {
		return new VersionChange(VersionOrigin.FIX_ACCEPTED, TestEventType.FIX_ACCEPTED, 42L, "ok", null);
	}

	@Test
	void newVersionGetsLineageAndAuthorAndSlotIsSwitched() {
		when(questionRepository.switchCurrentVersion(TENANT, 7L, 70L, 71L, QuestionStatus.OK)).thenReturn(true);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(List.of(
				QuestionFixtures.persisted(7L, 0, 71L, QuestionFixtures.defaultVersion())));

		TestQuestionVersion draft = flaggedQuestion.getCurrentVersion().toFixedDraft();
		tx.applyNewVersion(TENANT, flaggedQuestion, 70L, draft, fix());

		ArgumentCaptor<TestQuestionVersion> inserted = ArgumentCaptor.forClass(TestQuestionVersion.class);
		verify(questionRepository).insertVersion(inserted.capture());
		TestQuestionVersion v = inserted.getValue();
		assertThat(v.getVersionNo()).isEqualTo(2);
		assertThat(v.getParentVersionId()).isEqualTo(70L);
		assertThat(v.getQuestionId()).isEqualTo(7L);
		assertThat(v.getOrigin()).isEqualTo(VersionOrigin.FIX_ACCEPTED);
		assertThat(v.getCreatedBy()).isEqualTo(42L);
		assertThat(v.getCreatedAt()).isNotNull();
		verify(history).versionChanged(eq(flaggedQuestion), eq(TestEventType.FIX_ACCEPTED), eq(70L), eq(v), eq(42L),
				eq("ok"), any());
	}

	@Test
	void resolvingLastFlaggedQuestionMarksTestReviewed() {
		when(questionRepository.switchCurrentVersion(TENANT, 7L, 70L, 71L, QuestionStatus.OK)).thenReturn(true);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(List.of(
				QuestionFixtures.persisted(7L, 0, 71L, QuestionFixtures.defaultVersion())));

		tx.applyNewVersion(TENANT, flaggedQuestion, 70L, flaggedQuestion.getCurrentVersion().toFixedDraft(), fix());

		verify(testRepository).updateStatus(TENANT, TEST_ID, TestStatus.REVIEWED);
		verify(history).statusChanged(eq(TENANT), eq(TEST_ID), eq(TestStatus.REVIEWED), eq(42L), any());
	}

	@Test
	void testStaysInReviewWhileOtherQuestionsAreFlagged() {
		when(questionRepository.switchCurrentVersion(TENANT, 7L, 70L, 71L, QuestionStatus.OK)).thenReturn(true);
		TestQuestionVersion stillFlagged = QuestionFixtures.defaultVersion();
		stillFlagged.getOption(OptionLetter.C).flag("x", "r");
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(List.of(
				QuestionFixtures.persisted(7L, 0, 71L, QuestionFixtures.defaultVersion()),
				QuestionFixtures.persisted(8L, 1, 80L, stillFlagged)));

		tx.applyNewVersion(TENANT, flaggedQuestion, 70L, flaggedQuestion.getCurrentVersion().toFixedDraft(), fix());

		verify(testRepository, never()).updateStatus(anyLong(), anyLong(), any());
	}

	@Test
	void regeneratedFlaggedQuestionReopensReviewedTest() {
		when(testRepository.findByTenantIdAndIdForUpdate(TENANT, TEST_ID))
				.thenReturn(Optional.of(QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT, TestStatus.REVIEWED)));
		TestQuestionVersion regenerated = QuestionFixtures.defaultVersion();
		regenerated.getOption(OptionLetter.D).flag("y", "r");
		when(questionRepository.switchCurrentVersion(TENANT, 7L, 70L, 71L, QuestionStatus.NEEDS_REVIEW))
				.thenReturn(true);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID))
				.thenReturn(List.of(QuestionFixtures.persisted(7L, 0, 71L, regenerated)));

		tx.applyNewVersion(TENANT, flaggedQuestion, 70L, regenerated, new VersionChange(VersionOrigin.REGENERATED,
				TestEventType.QUESTION_REGENERATED, 42L, null, null));

		verify(testRepository).updateStatus(TENANT, TEST_ID, TestStatus.REQUIRES_REVIEW);
	}

	@Test
	void concurrentChangeIsAConflict() {
		when(questionRepository.switchCurrentVersion(TENANT, 7L, 70L, 71L, QuestionStatus.OK)).thenReturn(false);

		assertThatThrownBy(() -> tx.applyNewVersion(TENANT, flaggedQuestion, 70L,
				flaggedQuestion.getCurrentVersion().toFixedDraft(), fix())).isInstanceOf(ReviewConflictException.class);
		verify(history, never()).versionChanged(any(), any(), any(), any(), any(), any(), any());
	}

	// ---------------- publish (ADR005) ----------------

	private void givenLockedTest(TestStatus status) {
		when(testRepository.findByTenantIdAndIdForUpdate(TENANT, TEST_ID))
				.thenReturn(Optional.of(QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT, status)));
	}

	@Test
	void reviewActionOnPublishedTestIsRejectedUnderTheLock() {
		givenLockedTest(TestStatus.PUBLISHED);

		assertThatThrownBy(() -> tx.applyNewVersion(TENANT, flaggedQuestion, 70L,
				flaggedQuestion.getCurrentVersion().toFixedDraft(), fix()))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("PUBLISHED");
		verify(questionRepository, never()).insertVersion(any());
		verify(questionRepository, never()).switchCurrentVersion(anyLong(), anyLong(), anyLong(), anyLong(), any());
	}

	@Test
	void reviewedTestIsPublishedWithAnEventByTheActor() {
		givenLockedTest(TestStatus.REVIEWED);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(List.of(
				QuestionFixtures.persisted(7L, 0, 71L, QuestionFixtures.defaultVersion())));

		var published = tx.publish(TENANT, TEST_ID, 42L, "approved");

		assertThat(published.getStatus()).isEqualTo(TestStatus.PUBLISHED);
		verify(testRepository).updateStatus(TENANT, TEST_ID, TestStatus.PUBLISHED);
		verify(history).statusChanged(eq(TENANT), eq(TEST_ID), eq(TestStatus.PUBLISHED), eq(42L), eq("approved"),
				any());
	}

	@Test
	void testRequiringReviewCannotBePublished() {
		givenLockedTest(TestStatus.REQUIRES_REVIEW);

		assertThatThrownBy(() -> tx.publish(TENANT, TEST_ID, 42L, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("REQUIRES_REVIEW");
		verify(testRepository, never()).updateStatus(anyLong(), anyLong(), any());
	}

	@Test
	void flaggedQuestionBlocksPublishEvenIfStatusSaysGenerated() {
		givenLockedTest(TestStatus.GENERATED);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(List.of(flaggedQuestion));

		assertThatThrownBy(() -> tx.publish(TENANT, TEST_ID, 42L, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("need review");
		verify(testRepository, never()).updateStatus(anyLong(), anyLong(), any());
	}

	@Test
	void publishingTwiceIsAConflict() {
		givenLockedTest(TestStatus.PUBLISHED);

		assertThatThrownBy(() -> tx.publish(TENANT, TEST_ID, 42L, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("already published");
	}

	// ---------------- unpublish (ADR005) ----------------

	private void givenPublishEvent(String previousStatus) {
		TestEvent published = TestEvent.builder().tenantId(TENANT).testId(TEST_ID)
				.eventType(TestEventType.TEST_PUBLISHED).payload(Map.of("previousStatus", previousStatus)).build();
		TestEvent created = TestEvent.builder().tenantId(TENANT).testId(TEST_ID)
				.eventType(TestEventType.TEST_CREATED).build();
		when(eventRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(List.of(created, published));
	}

	@Test
	void unpublishRestoresTheStatusBeforePublishing() {
		givenLockedTest(TestStatus.PUBLISHED);
		givenPublishEvent("GENERATED");

		var test = tx.unpublish(TENANT, TEST_ID, 42L, "typo in question 3");

		assertThat(test.getStatus()).isEqualTo(TestStatus.GENERATED);
		verify(testRepository).updateStatus(TENANT, TEST_ID, TestStatus.GENERATED);
		verify(history).testEvent(eq(TENANT), eq(TEST_ID), eq(TestEventType.TEST_UNPUBLISHED), eq(42L),
				eq("typo in question 3"), eq(Map.of("restoredStatus", "GENERATED")));
	}

	@Test
	void unpublishFallsBackToReviewedWithoutAUsablePublishEvent() {
		givenLockedTest(TestStatus.PUBLISHED);
		givenPublishEvent("REQUIRES_REVIEW");

		assertThat(tx.unpublish(TENANT, TEST_ID, 42L, null).getStatus()).isEqualTo(TestStatus.REVIEWED);
		verify(testRepository).updateStatus(TENANT, TEST_ID, TestStatus.REVIEWED);
	}

	@Test
	void testWithAttemptsCannotBeUnpublished() {
		givenLockedTest(TestStatus.PUBLISHED);
		when(attemptRepository.existsByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(true);

		assertThatThrownBy(() -> tx.unpublish(TENANT, TEST_ID, 42L, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("has attempts");
		verify(testRepository, never()).updateStatus(anyLong(), anyLong(), any());
		verify(history, never()).testEvent(anyLong(), anyLong(), any(), any(), any(), any());
	}

	@Test
	void testThatIsNotPublishedCannotBeUnpublished() {
		givenLockedTest(TestStatus.REVIEWED);

		assertThatThrownBy(() -> tx.unpublish(TENANT, TEST_ID, 42L, null))
				.isInstanceOf(ReviewConflictException.class).hasMessageContaining("not published");
	}
}
