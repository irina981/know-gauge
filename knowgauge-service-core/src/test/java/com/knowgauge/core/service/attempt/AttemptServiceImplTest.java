package com.knowgauge.core.service.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.AttemptConflictException;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.AttemptStatus;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.AttemptAnswerRepository;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.core.port.repository.TestInvitationRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.attempt.AttemptService.AttemptView;
import com.knowgauge.core.support.QuestionFixtures;

class AttemptServiceImplTest {

	private static final long TENANT = QuestionFixtures.TENANT;
	private static final long TEST_ID = QuestionFixtures.TEST_ID;
	private static final long USER = 7L;
	private static final long ATTEMPT_ID = 500L;

	private AttemptRepository attemptRepository;
	private AttemptAnswerRepository answerRepository;
	private TestRepository testRepository;
	private TestQuestionRepository questionRepository;
	private TestInvitationRepository invitationRepository;
	private ApplicationEventPublisher events;
	private TestParticipants participants;
	private AttemptServiceImpl service;

	/** Q1: answer A. Q2: answer B. */
	private List<TestQuestion> singleCorrectQuestions;

	@BeforeEach
	void setUp() {
		attemptRepository = mock(AttemptRepository.class);
		answerRepository = mock(AttemptAnswerRepository.class);
		testRepository = mock(TestRepository.class);
		questionRepository = mock(TestQuestionRepository.class);
		ExecutionContext context = mock(ExecutionContext.class);
		when(context.tenantId()).thenReturn(TENANT);
		when(context.userId()).thenReturn(OptionalLong.of(USER));
		service = new AttemptServiceImpl(attemptRepository, answerRepository, testRepository, questionRepository,
				invitationRepository = mock(TestInvitationRepository.class), context,
				events = mock(ApplicationEventPublisher.class), participants = mock(TestParticipants.class));

		singleCorrectQuestions = List.of(
				QuestionFixtures.persisted(1L, 0, 11L, QuestionFixtures.defaultVersion()),
				QuestionFixtures.persisted(2L, 1, 21L, QuestionFixtures.version("Which is a set?",
						Set.of(OptionLetter.B), "ArrayList", "HashSet", "LinkedList", "Vector")));
		when(attemptRepository.save(any())).thenAnswer(inv -> {
			Attempt a = inv.getArgument(0);
			if (a.getId() == null) {
				a.setId(ATTEMPT_ID);
			}
			return a;
		});
		when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
	}

	// ---------------- start ----------------

	@Test
	void startCreatesAnAttemptForThePublishedTestAndCurrentUser() {
		givenTest(AnswerCardinality.SINGLE_CORRECT, TestStatus.PUBLISHED);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(singleCorrectQuestions);

		AttemptView view = service.start(TEST_ID);

		assertThat(view.attempt().getStatus()).isEqualTo(AttemptStatus.STARTED);
		assertThat(view.attempt().getUserId()).isEqualTo(USER);
		assertThat(view.attempt().getTotalQuestions()).isEqualTo(2);
		assertThat(view.attempt().getStartedAt()).isNotNull();
		assertThat(view.questions()).hasSize(2);
		assertThat(view.answers()).isEmpty();
	}

	@Test
	void startRejectsTestsThatAreNotPublished() {
		givenTest(AnswerCardinality.SINGLE_CORRECT, TestStatus.REVIEWED);

		assertThatThrownBy(() -> service.start(TEST_ID)).isInstanceOf(AttemptConflictException.class)
				.hasMessageContaining("not published");
		verify(attemptRepository, never()).save(any());
	}

	@Test
	void startRejectsUnknownTests() {
		when(testRepository.findByTenantIdAndIdForUpdate(TENANT, TEST_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.start(TEST_ID)).isInstanceOf(ResourceNotFoundException.class);
	}

	// ---------------- participants (ADR009) ----------------

	private static final String LINK = "test-link-token";
	private static final String INVITE = "invitation-token";
	private static final long INVITATION_ID = 12L;

	@Test
	void sharedLinkStartsAnAttemptWithTheNormalizedNameAndEmail() {
		givenPublishedTestBehindLink();

		AttemptView view = service.startForParticipant(LINK, "  Ana   Petrovic ", " Ana@Example.COM ");

		assertThat(view.attempt().getParticipantName()).isEqualTo("Ana Petrovic");
		assertThat(view.attempt().getParticipantEmail()).isEqualTo("ana@example.com");
		assertThat(view.attempt().getUserId()).isNull();
		assertThat(view.attempt().getPublicToken()).isNotBlank();
		assertThat(view.attempt().getInvitationId()).isNull();
	}

	@Test
	void sharedLinkAllowsAnyNumberOfAttemptsForTheSameEmail() {
		givenPublishedTestBehindLink();

		service.startForParticipant(LINK, "Ana", "ana@example.com");
		service.startForParticipant(LINK, "Ana", "ana@example.com");

		verify(attemptRepository, org.mockito.Mockito.times(2)).save(any());
	}

	@Test
	void sharedLinkRefusesANewPersonOnceTheTestIsFull() {
		givenPublishedTestBehindLink();
		org.mockito.Mockito.doThrow(new com.knowgauge.core.exception.ParticipantLimitReachedException("full"))
				.when(participants).requireRoomForParticipant(TENANT, TEST_ID, "ana@example.com");

		assertThatThrownBy(() -> service.startForParticipant(LINK, "Ana", "Ana@Example.com"))
				.isInstanceOf(com.knowgauge.core.exception.ParticipantLimitReachedException.class);
		verify(attemptRepository, never()).save(any());
	}

	@Test
	void sharedLinkRejectsAMissingNameOrInvalidEmail() {
		givenPublishedTestBehindLink();

		assertThatThrownBy(() -> service.startForParticipant(LINK, "  ", "ana@example.com"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("full name");
		assertThatThrownBy(() -> service.startForParticipant(LINK, "Ana", "not-an-email"))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("valid email");
		verify(attemptRepository, never()).save(any());
	}

	@Test
	void sharedLinkOfAnUnpublishedTestDoesNotWork() {
		com.knowgauge.core.model.Test test = QuestionFixtures.test(AnswerCardinality.SINGLE_CORRECT,
				TestStatus.REVIEWED);
		when(testRepository.findByPublicToken(LINK)).thenReturn(Optional.of(test));

		assertThatThrownBy(() -> service.startForParticipant(LINK, "Ana", "ana@example.com"))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("not valid or the test is closed");
	}

	@Test
	void invitationTakesTheEmailFromTheInvitation() {
		givenPublishedTestBehindLink();
		givenInvitation();

		AttemptView view = service.startForInvitation(INVITE, "Ana Petrovic");

		assertThat(view.attempt().getParticipantEmail()).isEqualTo("invited@example.com");
		assertThat(view.attempt().getParticipantName()).isEqualTo("Ana Petrovic");
		assertThat(view.attempt().getInvitationId()).isEqualTo(INVITATION_ID);
	}

	@Test
	void invitationContinuesTheRunningAttemptInsteadOfStartingAnother() {
		givenPublishedTestBehindLink();
		givenInvitation();
		Attempt running = Attempt.builder().id(ATTEMPT_ID).tenantId(TENANT).testId(TEST_ID)
				.invitationId(INVITATION_ID).status(AttemptStatus.STARTED).totalQuestions(2).build();
		when(attemptRepository.findByTenantIdAndInvitationId(TENANT, INVITATION_ID)).thenReturn(Optional.of(running));

		AttemptView view = service.startForInvitation(INVITE, "Ana");

		assertThat(view.attempt()).isSameAs(running);
		verify(attemptRepository, never()).save(any());
	}

	@Test
	void invitationCanBeUsedOnlyOnce() {
		givenPublishedTestBehindLink();
		givenInvitation();
		Attempt scored = Attempt.builder().id(ATTEMPT_ID).tenantId(TENANT).testId(TEST_ID)
				.invitationId(INVITATION_ID).status(AttemptStatus.SCORED).build();
		when(attemptRepository.findByTenantIdAndInvitationId(TENANT, INVITATION_ID)).thenReturn(Optional.of(scored));

		assertThatThrownBy(() -> service.startForInvitation(INVITE, "Ana"))
				.isInstanceOf(AttemptConflictException.class).hasMessageContaining("already used");
		verify(attemptRepository, never()).save(any());
	}

	private void givenPublishedTestBehindLink() {
		givenTest(AnswerCardinality.SINGLE_CORRECT, TestStatus.PUBLISHED);
		com.knowgauge.core.model.Test test = testRepository.findByTenantIdAndId(TENANT, TEST_ID).orElseThrow();
		when(testRepository.findByPublicToken(LINK)).thenReturn(Optional.of(test));
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(singleCorrectQuestions);
	}

	private void givenInvitation() {
		TestInvitation invitation = TestInvitation.builder().id(INVITATION_ID).tenantId(TENANT).testId(TEST_ID)
				.email("invited@example.com").publicToken(INVITE).build();
		when(invitationRepository.findByPublicToken(INVITE)).thenReturn(Optional.of(invitation));
	}

	// ---------------- submit ----------------

	@Test
	void submitScoresEachQuestionAndTheAttempt() {
		givenStartedAttempt(AnswerCardinality.SINGLE_CORRECT, singleCorrectQuestions);

		AttemptView view = service.submit(ATTEMPT_ID,
				Map.of(1L, List.of(OptionLetter.A), 2L, List.of(OptionLetter.C)));

		Attempt attempt = view.attempt();
		assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SCORED);
		assertThat(attempt.getCorrectCount()).isEqualTo(1);
		assertThat(attempt.getWrongCount()).isEqualTo(1);
		assertThat(attempt.getScorePercent()).isEqualTo(50.0);
		assertThat(attempt.getSubmittedAt()).isNotNull();
		assertThat(attempt.getScoredAt()).isNotNull();

		List<AttemptAnswer> answers = savedAnswers();
		assertThat(answers).extracting(AttemptAnswer::getCorrect).containsExactly(true, false);
		assertThat(answers).extracting(AttemptAnswer::getQuestionVersionId).containsExactly(11L, 21L);
		assertThat(answers.get(1).getChosenOptions()).containsExactly(OptionLetter.C);
		// The result email goes out after commit (AttemptResultNotifier).
		ArgumentCaptor<AttemptScoredEvent> event = ArgumentCaptor.forClass(AttemptScoredEvent.class);
		verify(events).publishEvent(event.capture());
		assertThat(event.getValue().attempt()).isSameAs(attempt);
	}

	@Test
	void unansweredQuestionsCountAsWrong() {
		givenStartedAttempt(AnswerCardinality.SINGLE_CORRECT, singleCorrectQuestions);

		AttemptView view = service.submit(ATTEMPT_ID, Map.of(1L, List.of(OptionLetter.A)));

		assertThat(view.attempt().getCorrectCount()).isEqualTo(1);
		assertThat(view.attempt().getWrongCount()).isEqualTo(1);
		List<AttemptAnswer> answers = savedAnswers();
		assertThat(answers).hasSize(2);
		assertThat(answers.get(1).getChosenOptions()).isEmpty();
		assertThat(answers.get(1).getCorrect()).isFalse();
	}

	@Test
	void multipleCorrectQuestionsNeedTheExactSetOfAnswers() {
		TestQuestion multi = QuestionFixtures.persisted(3L, 0, 31L, QuestionFixtures.version("Which are lists?",
				Set.of(OptionLetter.A, OptionLetter.C), "ArrayList", "HashSet", "LinkedList", "TreeMap"));
		givenStartedAttempt(AnswerCardinality.MULTIPLE_CORRECT, List.of(multi));

		AttemptView partial = service.submit(ATTEMPT_ID, Map.of(3L, List.of(OptionLetter.A)));
		assertThat(partial.attempt().getCorrectCount()).isZero();

		givenStartedAttempt(AnswerCardinality.MULTIPLE_CORRECT, List.of(multi));
		AttemptView exact = service.submit(ATTEMPT_ID, Map.of(3L, List.of(OptionLetter.C, OptionLetter.A)));
		assertThat(exact.attempt().getCorrectCount()).isEqualTo(1);
		assertThat(exact.attempt().getScorePercent()).isEqualTo(100.0);
	}

	@Test
	void submitRejectsAnAttemptThatWasAlreadySubmitted() {
		givenStartedAttempt(AnswerCardinality.SINGLE_CORRECT, singleCorrectQuestions);
		Attempt scored = Attempt.builder().id(ATTEMPT_ID).tenantId(TENANT).testId(TEST_ID)
				.status(AttemptStatus.SCORED).build();
		when(attemptRepository.findByTenantIdAndIdForUpdate(TENANT, ATTEMPT_ID)).thenReturn(Optional.of(scored));

		assertThatThrownBy(() -> service.submit(ATTEMPT_ID, Map.of())).isInstanceOf(AttemptConflictException.class);
		verify(answerRepository, never()).saveAll(anyList());
	}

	@Test
	void submitRejectsQuestionsFromAnotherTest() {
		givenStartedAttempt(AnswerCardinality.SINGLE_CORRECT, singleCorrectQuestions);

		assertThatThrownBy(() -> service.submit(ATTEMPT_ID, Map.of(99L, List.of(OptionLetter.A))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("99");
		verify(answerRepository, never()).saveAll(anyList());
	}

	@Test
	void submitRejectsSeveralAnswersOnASingleAnswerTest() {
		givenStartedAttempt(AnswerCardinality.SINGLE_CORRECT, singleCorrectQuestions);

		assertThatThrownBy(() -> service.submit(ATTEMPT_ID, Map.of(1L, List.of(OptionLetter.A, OptionLetter.B))))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("only one answer");
	}

	// ---------------- helpers ----------------

	private void givenTest(AnswerCardinality cardinality, TestStatus status) {
		com.knowgauge.core.model.Test test = QuestionFixtures.test(cardinality, status);
		when(testRepository.findByTenantIdAndIdForUpdate(TENANT, TEST_ID)).thenReturn(Optional.of(test));
		when(testRepository.findByTenantIdAndId(TENANT, TEST_ID)).thenReturn(Optional.of(test));
	}

	private void givenStartedAttempt(AnswerCardinality cardinality, List<TestQuestion> questions) {
		givenTest(cardinality, TestStatus.PUBLISHED);
		when(questionRepository.findByTenantIdAndTestId(TENANT, TEST_ID)).thenReturn(questions);
		Attempt started = Attempt.builder().id(ATTEMPT_ID).tenantId(TENANT).testId(TEST_ID).userId(USER)
				.status(AttemptStatus.STARTED).totalQuestions(questions.size()).build();
		when(attemptRepository.findByTenantIdAndIdForUpdate(TENANT, ATTEMPT_ID)).thenReturn(Optional.of(started));
	}

	@SuppressWarnings("unchecked")
	private List<AttemptAnswer> savedAnswers() {
		ArgumentCaptor<List<AttemptAnswer>> captor = ArgumentCaptor.forClass(List.class);
		verify(answerRepository).saveAll(captor.capture());
		return captor.getValue();
	}
}
