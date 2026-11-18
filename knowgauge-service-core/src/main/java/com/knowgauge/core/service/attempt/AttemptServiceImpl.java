package com.knowgauge.core.service.attempt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.AttemptConflictException;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestInvitation;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.AttemptStatus;
import com.knowgauge.core.model.enums.OptionLetter;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.AttemptAnswerRepository;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.core.port.repository.TestInvitationRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.util.Emails;
import com.knowgauge.core.util.PublicTokens;

import lombok.extern.slf4j.Slf4j;

/**
 * Attempts only touch the database (no external calls), so each method is one
 * short transaction.
 */
@Slf4j
@Service
public class AttemptServiceImpl implements AttemptService {

	private final AttemptRepository attemptRepository;
	private final AttemptAnswerRepository answerRepository;
	private final TestRepository testRepository;
	private final TestQuestionRepository questionRepository;
	private final TestInvitationRepository invitationRepository;
	private final ExecutionContext executionContext;
	private final ApplicationEventPublisher events;

	public AttemptServiceImpl(AttemptRepository attemptRepository, AttemptAnswerRepository answerRepository,
			TestRepository testRepository, TestQuestionRepository questionRepository,
			TestInvitationRepository invitationRepository, ExecutionContext executionContext,
			ApplicationEventPublisher events) {
		this.attemptRepository = attemptRepository;
		this.answerRepository = answerRepository;
		this.testRepository = testRepository;
		this.questionRepository = questionRepository;
		this.invitationRepository = invitationRepository;
		this.executionContext = executionContext;
		this.events = events;
	}

	// ---------------- public (participants, ADR009) ----------------
	// Participants are anonymous, so there is no tenant in the context: the
	// tenant is the one of the test, invitation or attempt the token belongs to.

	@Override
	@Transactional(readOnly = true)
	public Test getPublishedTest(String testToken) {
		return publishedTestByToken(testToken);
	}

	@Override
	@Transactional
	public AttemptView startForParticipant(String testToken, String participantName, String participantEmail) {
		String name = normalizeName(participantName);
		String email = Emails.normalize(participantEmail);
		Test test = publishedTestByToken(testToken);
		return startAttempt(test.getTenantId(), test.getId(),
				Attempt.builder().participantName(name).participantEmail(email));
	}

	@Override
	@Transactional
	public AttemptView startForInvitation(String invitationToken, String participantName) {
		String name = normalizeName(participantName);
		TestInvitation invitation = invitationRepository.findByPublicToken(invitationToken)
				.orElseThrow(() -> new ResourceNotFoundException("This invitation link is not valid."));
		long tenantId = invitation.getTenantId();
		// Lock the test first, so two clicks on the same link cannot both start an attempt.
		Test test = testRepository.findByTenantIdAndIdForUpdate(tenantId, invitation.getTestId())
				.filter(t -> t.getStatus() == TestStatus.PUBLISHED)
				.orElseThrow(() -> new ResourceNotFoundException("This test is closed."));
		Optional<Attempt> existing = attemptRepository.findByTenantIdAndInvitationId(tenantId, invitation.getId());
		if (existing.isPresent()) {
			if (existing.get().getStatus() != AttemptStatus.STARTED) {
				throw new AttemptConflictException("This invitation was already used.");
			}
			return view(tenantId, existing.get());
		}
		// The email is the invited address, never what the participant types.
		return startAttempt(tenantId, test.getId(), Attempt.builder().participantName(name)
				.participantEmail(invitation.getEmail()).invitationId(invitation.getId()));
	}

	@Override
	@Transactional(readOnly = true)
	public AttemptView getByToken(String attemptToken) {
		Attempt attempt = attemptByToken(attemptToken);
		return view(attempt.getTenantId(), attempt);
	}

	@Override
	@Transactional
	public AttemptView submitByToken(String attemptToken, Map<Long, List<OptionLetter>> answers) {
		Attempt attempt = attemptByToken(attemptToken);
		return submit(attempt.getTenantId(), attempt.getId(), answers);
	}

	// ---------------- admin ----------------

	@Override
	@Transactional
	public AttemptView start(Long testId) {
		return startAttempt(executionContext.tenantId(), testId, Attempt.builder().userId(currentUserId()));
	}

	/**
	 * Locks the test row so an unpublish cannot slip in between the status check
	 * and the insert (unpublish refuses tests that have attempts, under the same
	 * lock). {@code identity} carries who takes the test.
	 */
	private AttemptView startAttempt(long tenantId, Long testId, Attempt.AttemptBuilder<?, ?> identity) {
		Test test = testRepository.findByTenantIdAndIdForUpdate(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
		if (test.getStatus() != TestStatus.PUBLISHED) {
			throw new AttemptConflictException("Test " + testId + " is not published (status " + test.getStatus()
					+ "). Only published tests can be taken.");
		}
		List<TestQuestion> questions = questionRepository.findByTenantIdAndTestId(tenantId, testId);
		if (questions.isEmpty()) {
			throw new AttemptConflictException("Test " + testId + " has no questions.");
		}

		Attempt attempt = attemptRepository.save(identity.tenantId(tenantId).testId(testId)
				.publicToken(PublicTokens.newToken()).status(AttemptStatus.STARTED).totalQuestions(questions.size())
				.startedAt(Instant.now()).build());
		log.info("Attempt {} started on test {} ({} questions)", attempt.getId(), testId, questions.size());
		return new AttemptView(attempt, test, questions, List.of());
	}

	@Override
	@Transactional(readOnly = true)
	public AttemptView get(Long attemptId) {
		long tenantId = executionContext.tenantId();
		Attempt attempt = attemptRepository.findByTenantIdAndId(tenantId, attemptId)
				.orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
		return view(tenantId, attempt);
	}

	@Override
	@Transactional
	public AttemptView submit(Long attemptId, Map<Long, List<OptionLetter>> chosen) {
		return submit(executionContext.tenantId(), attemptId, chosen);
	}

	private AttemptView submit(long tenantId, Long attemptId, Map<Long, List<OptionLetter>> chosen) {
		Attempt attempt = attemptRepository.findByTenantIdAndIdForUpdate(tenantId, attemptId)
				.orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));
		if (attempt.getStatus() != AttemptStatus.STARTED) {
			throw new AttemptConflictException("Attempt " + attemptId + " was already submitted.");
		}
		Test test = testRepository.findByTenantIdAndId(tenantId, attempt.getTestId())
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + attempt.getTestId()));
		List<TestQuestion> questions = questionRepository.findByTenantIdAndTestId(tenantId, attempt.getTestId());

		Set<Long> questionIds = questions.stream().map(TestQuestion::getId).collect(Collectors.toSet());
		Set<Long> unknown = new HashSet<>(chosen.keySet());
		unknown.removeAll(questionIds);
		if (!unknown.isEmpty()) {
			throw new IllegalArgumentException("Questions " + unknown + " are not part of test " + test.getId() + ".");
		}

		Instant now = Instant.now();
		List<AttemptAnswer> answers = new ArrayList<>(questions.size());
		int correctCount = 0;
		for (TestQuestion question : questions) {
			EnumSet<OptionLetter> picked = toSet(chosen.get(question.getId()));
			if (test.getAnswerCardinality() == AnswerCardinality.SINGLE_CORRECT && picked.size() > 1) {
				throw new IllegalArgumentException("Question " + question.getId()
						+ " allows only one answer, but " + picked.size() + " were chosen.");
			}
			boolean correct = !picked.isEmpty() && picked.equals(toSet(question.getCorrectOptions()));
			if (correct) {
				correctCount++;
			}
			answers.add(AttemptAnswer.builder().tenantId(tenantId).attemptId(attemptId).questionId(question.getId())
					.questionVersionId(question.getCurrentVersionId()).chosenOptions(new ArrayList<>(picked))
					.correct(correct).build());
		}
		List<AttemptAnswer> saved = answerRepository.saveAll(answers);

		int total = questions.size();
		attempt.setTotalQuestions(total);
		attempt.setCorrectCount(correctCount);
		attempt.setWrongCount(total - correctCount);
		attempt.setScorePercent(Math.round(correctCount * 1000.0 / total) / 10.0);
		attempt.setStatus(AttemptStatus.SCORED);
		attempt.setSubmittedAt(now);
		attempt.setScoredAt(now);
		Attempt scored = attemptRepository.save(attempt);
		// Emails the participant once this transaction commits (AttemptResultNotifier).
		events.publishEvent(new AttemptScoredEvent(scored, test.getName()));
		log.info("Attempt {} on test {} scored: {}/{} correct ({}%)", attemptId, test.getId(), correctCount, total,
				scored.getScorePercent());
		return new AttemptView(scored, test, questionsAsAnswered(tenantId, questions, saved), saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Attempt> list(Long testId, Long userId, String participantEmail) {
		// Stored lower-cased (Emails), so a plain comparison is case-insensitive.
		String email = participantEmail == null || participantEmail.isBlank() ? null
				: participantEmail.trim().toLowerCase(java.util.Locale.ROOT);
		return attemptRepository.findByTenantId(executionContext.tenantId(), testId, userId, email);
	}

	private Test publishedTestByToken(String testToken) {
		// Unknown token and unpublished test look the same to the participant: the link doesn't work.
		return testRepository.findByPublicToken(testToken)
				.filter(test -> test.getStatus() == TestStatus.PUBLISHED)
				.orElseThrow(() -> new ResourceNotFoundException("This test link is not valid or the test is closed."));
	}

	private Attempt attemptByToken(String attemptToken) {
		return attemptRepository.findByPublicToken(attemptToken)
				.orElseThrow(() -> new ResourceNotFoundException("Attempt not found."));
	}

	private static String normalizeName(String name) {
		String trimmed = name == null ? "" : name.trim().replaceAll("\\s+", " ");
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Please enter your full name.");
		}
		if (trimmed.length() > 200) {
			throw new IllegalArgumentException("The name can be at most 200 characters.");
		}
		return trimmed;
	}


	private AttemptView view(long tenantId, Attempt attempt) {
		Test test = testRepository.findByTenantIdAndId(tenantId, attempt.getTestId())
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + attempt.getTestId()));
		List<TestQuestion> questions = questionRepository.findByTenantIdAndTestId(tenantId, attempt.getTestId());
		List<AttemptAnswer> answers = attempt.getStatus() == AttemptStatus.STARTED ? List.of()
				: answerRepository.findByTenantIdAndAttemptId(tenantId, attempt.getId());
		return new AttemptView(attempt, test, questionsAsAnswered(tenantId, questions, answers), answers);
	}

	/**
	 * Published tests are frozen, so the current version is normally the one that
	 * was answered. If a question moved on anyway, show the version that was
	 * actually answered, so the review matches the score.
	 */
	private List<TestQuestion> questionsAsAnswered(long tenantId, List<TestQuestion> questions,
			List<AttemptAnswer> answers) {
		Map<Long, Long> answeredVersion = answers.stream()
				.collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getQuestionVersionId));
		List<TestQuestion> result = new ArrayList<>(questions.size());
		for (TestQuestion question : questions) {
			Long versionId = answeredVersion.get(question.getId());
			if (versionId == null || versionId.equals(question.getCurrentVersionId())) {
				result.add(question);
				continue;
			}
			Map<Long, TestQuestionVersion> versions = questionRepository.findVersions(tenantId, question.getId())
					.stream().collect(Collectors.toMap(TestQuestionVersion::getId, Function.identity(),
							(a, b) -> a, LinkedHashMap::new));
			TestQuestionVersion answered = versions.get(versionId);
			if (answered == null) {
				result.add(question);
				continue;
			}
			result.add(TestQuestion.builder().id(question.getId()).tenantId(question.getTenantId())
					.testId(question.getTestId()).questionIndex(question.getQuestionIndex())
					.status(question.getStatus()).currentVersionId(versionId).currentVersion(answered).build());
		}
		return result;
	}

	private long currentUserId() {
		return executionContext.userId().orElseThrow(
				() -> new IllegalStateException("No current user; attempts need kg.context.user-id to be set."));
	}

	private static EnumSet<OptionLetter> toSet(List<OptionLetter> letters) {
		EnumSet<OptionLetter> set = EnumSet.noneOf(OptionLetter.class);
		if (letters != null) {
			set.addAll(letters);
		}
		return set;
	}
}
