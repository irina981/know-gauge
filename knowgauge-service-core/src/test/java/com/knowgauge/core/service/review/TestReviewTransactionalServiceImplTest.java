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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.exception.ReviewConflictException;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.QuestionStatus;
import com.knowgauge.core.model.enums.TestEventType;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.model.enums.VersionOrigin;
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
	private TestHistoryRecorder history;
	private TestReviewTransactionalServiceImpl tx;

	private TestQuestion flaggedQuestion;

	@BeforeEach
	void setUp() {
		testRepository = mock(TestRepository.class);
		questionRepository = mock(TestQuestionRepository.class);
		history = mock(TestHistoryRecorder.class);
		tx = new TestReviewTransactionalServiceImpl(testRepository, questionRepository, history);

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
		when(testRepository.findByTenantIdAndId(TENANT, TEST_ID)).thenReturn(
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
		when(testRepository.findByTenantIdAndId(TENANT, TEST_ID))
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
}
