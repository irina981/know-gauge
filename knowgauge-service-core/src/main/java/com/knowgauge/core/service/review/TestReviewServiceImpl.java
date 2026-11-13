package com.knowgauge.core.service.review;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.QuestionValidationException;
import com.knowgauge.core.exception.RegenerationFailedException;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.exception.ReviewConflictException;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestEvent;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
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

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates review actions (not transactional: regeneration calls the LLM).
 * All writes go through {@link TestReviewTransactionalServiceImpl}.
 */
@Slf4j
@Service
public class TestReviewServiceImpl implements TestReviewService {

	private final TestRepository testRepository;
	private final TestQuestionRepository questionRepository;
	private final TestEventRepository eventRepository;
	private final GenerationRunRepository generationRunRepository;
	private final DocumentChunkRepository chunkRepository;
	private final QuestionBatchGenerator batchGenerator;
	private final TestQuestionValidator validator;
	private final TestReviewTransactionalServiceImpl tx;
	private final ExecutionContext executionContext;

	public TestReviewServiceImpl(TestRepository testRepository, TestQuestionRepository questionRepository,
			TestEventRepository eventRepository, GenerationRunRepository generationRunRepository,
			DocumentChunkRepository chunkRepository, QuestionBatchGenerator batchGenerator,
			TestQuestionValidator validator, TestReviewTransactionalServiceImpl tx,
			ExecutionContext executionContext) {
		this.testRepository = testRepository;
		this.questionRepository = questionRepository;
		this.eventRepository = eventRepository;
		this.generationRunRepository = generationRunRepository;
		this.chunkRepository = chunkRepository;
		this.batchGenerator = batchGenerator;
		this.validator = validator;
		this.tx = tx;
		this.executionContext = executionContext;
	}

	private record ReviewContext(Long tenantId, Long actorUserId, Test test, TestQuestion question) {
		TestQuestionVersion current() {
			return question.getCurrentVersion();
		}
	}

	// ------------------------------------------------------------------
	// Actions
	// ------------------------------------------------------------------

	@Override
	public TestQuestion acceptFix(Long testId, Long questionId, Long expectedCurrentVersionId, String comment) {
		ReviewContext ctx = loadForAction(testId, questionId, expectedCurrentVersionId);
		TestQuestionVersion current = ctx.current();
		if (!current.isFixApplicable()) {
			throw new ReviewConflictException(!current.hasFlags()
					? "Question " + questionId + " has no flagged options to fix."
					: current.suggestionsRepeatOptions()
							? "The suggested replacement for question " + questionId
									+ " repeats an existing option. Regenerate or replace it manually."
							: "Question " + questionId
									+ " has flagged options without a suggested replacement. Regenerate or replace it manually.");
		}

		Map<String, Object> replaced = new LinkedHashMap<>();
		for (OptionLetter letter : current.getFlaggedOptions()) {
			TestQuestionOption option = current.getOption(letter);
			replaced.put(letter.name(), Map.of("from", option.getText(), "to", option.getSuggestedText().trim()));
		}
		TestQuestionVersion draft = current.toFixedDraft();
		draft.setGenerationRunId(null);
		draft.setVerificationRunId(current.getVerificationRunId());

		TestQuestion result = tx.applyNewVersion(ctx.tenantId(), ctx.question(), expectedCurrentVersionId, draft,
				new VersionChange(VersionOrigin.FIX_ACCEPTED, TestEventType.FIX_ACCEPTED, ctx.actorUserId(), comment,
						Map.of("replacedOptions", replaced)));
		log.info("Review: test {} question {} - fix accepted by user {} (version {} -> {})", testId, questionId,
				ctx.actorUserId(), expectedCurrentVersionId, result.getCurrentVersionId());
		return result;
	}

	@Override
	public TestQuestion regenerate(Long testId, Long questionId, Long expectedCurrentVersionId, String comment) {
		ReviewContext ctx = loadForAction(testId, questionId, expectedCurrentVersionId);
		Test test = ctx.test();
		TestQuestion question = ctx.question();

		List<Long> chunkIds = testRepository.findUsedChunkIds(ctx.tenantId(), testId);
		List<DocumentChunk> chunks = chunkIds.isEmpty() ? List.of()
				: chunkRepository.findByTenantIdAndIdIn(ctx.tenantId(), chunkIds);
		if (chunks.isEmpty()) {
			throw regenerationFailed(ctx, comment,
					"The test's source chunks are no longer available (documents were re-ingested or deleted).");
		}

		List<TestQuestion> others = otherQuestions(ctx);
		List<String> avoidTexts = new ArrayList<>();
		avoidTexts.add(question.getQuestionText());
		others.forEach(o -> avoidTexts.add(o.getQuestionText()));

		// Keep the slot's kind: a multi-correct question is replaced by a multi-correct one
		int minMulti = test.getAnswerCardinality() == AnswerCardinality.MULTIPLE_CORRECT
				&& ctx.current().getCorrectOptions().size() >= 2 ? 1 : 0;

		List<TestQuestion> candidates;
		try {
			candidates = batchGenerator.generateBatch(new BatchRequest(test, chunks, chunkIds, 1, minMulti,
					question.getQuestionIndex(), 1, GenerationRunPurpose.REGENERATION, questionId, avoidTexts));
		} catch (RuntimeException ex) {
			throw regenerationFailed(ctx, comment, "LLM call failed: " + ex.getMessage());
		}

		TestQuestion candidate = candidates.isEmpty() ? null : candidates.get(0);
		if (candidate == null) {
			throw regenerationFailed(ctx, comment, "The LLM did not produce a question that passed validation.");
		}
		List<TestQuestion> existing = new ArrayList<>(others);
		existing.add(question);
		if (validator.isDuplicateOfAny(candidate, existing)) {
			throw regenerationFailed(ctx, comment, "The LLM produced a duplicate of an existing question.");
		}

		TestQuestionVersion draft = candidate.getCurrentVersion();
		if (draft.getSourceChunkIds() == null || draft.getSourceChunkIds().isEmpty()) {
			draft.setSourceChunkIds(chunkIds);
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("generationRunId", draft.getGenerationRunId());
		payload.put("verificationRunId", draft.getVerificationRunId());
		payload.put("flaggedOptions", draft.getFlaggedOptions().stream().map(Enum::name).toList());

		TestQuestion result = tx.applyNewVersion(ctx.tenantId(), question, expectedCurrentVersionId, draft,
				new VersionChange(VersionOrigin.REGENERATED, TestEventType.QUESTION_REGENERATED, ctx.actorUserId(),
						comment, payload));
		log.info("Review: test {} question {} - regenerated by user {} (version {} -> {}, status {})", testId,
				questionId, ctx.actorUserId(), expectedCurrentVersionId, result.getCurrentVersionId(),
				result.getStatus());
		return result;
	}

	@Override
	public TestQuestion manualReplace(Long testId, Long questionId, ManualQuestionDraft input) {
		if (input == null) {
			throw new IllegalArgumentException("Manual question is required");
		}
		ReviewContext ctx = loadForAction(testId, questionId, input.expectedCurrentVersionId());

		TestQuestionVersion draft = new TestQuestionVersion();
		draft.setTenantId(ctx.tenantId());
		draft.setQuestionText(input.questionText());
		draft.setExplanation(input.explanation());
		List<Long> sources = ctx.current().getSourceChunkIds();
		draft.setSourceChunkIds(sources != null ? new ArrayList<>(sources) : null);
		EnumMap<OptionLetter, TestQuestionOption> options = new EnumMap<>(OptionLetter.class);
		List<String> missing = new ArrayList<>();
		for (OptionLetter letter : OptionLetter.values()) {
			ManualOption option = input.options() != null ? input.options().get(letter) : null;
			if (option == null) {
				missing.add("option " + letter + " is missing");
				continue;
			}
			options.put(letter, TestQuestionOption.builder().letter(letter).text(option.text())
					.correct(option.correct()).build());
		}
		if (!missing.isEmpty()) {
			throw new QuestionValidationException(missing);
		}
		draft.setOptions(options);

		TestQuestion candidate = TestQuestion.builder().tenantId(ctx.tenantId()).testId(testId).currentVersion(draft)
				.build();
		List<String> reasons = validator.validateManual(candidate, ctx.test(), otherQuestions(ctx));
		if (!reasons.isEmpty()) {
			throw new QuestionValidationException(reasons);
		}

		TestQuestion result = tx.applyNewVersion(ctx.tenantId(), ctx.question(), input.expectedCurrentVersionId(),
				draft, new VersionChange(VersionOrigin.MANUAL, TestEventType.QUESTION_MANUALLY_REPLACED,
						ctx.actorUserId(), input.comment(), null));
		log.info("Review: test {} question {} - manually replaced by user {} (version {} -> {})", testId, questionId,
				ctx.actorUserId(), input.expectedCurrentVersionId(), result.getCurrentVersionId());
		return result;
	}

	@Override
	public Test publish(Long testId, String comment) {
		Long actorUserId = actorUserId();
		Test published = tx.publish(executionContext.tenantId(), testId, actorUserId, comment);
		log.info("Review: test {} - published by user {}", testId, actorUserId);
		return published;
	}

	@Override
	public Test unpublish(Long testId, String comment) {
		Long actorUserId = actorUserId();
		Test unpublished = tx.unpublish(executionContext.tenantId(), testId, actorUserId, comment);
		log.info("Review: test {} - unpublished by user {} (back to {})", testId, actorUserId,
				unpublished.getStatus());
		return unpublished;
	}

	@Override
	public boolean hasAttempts(Long testId) {
		return tx.hasAttempts(executionContext.tenantId(), testId);
	}

	private Long actorUserId() {
		var userId = executionContext.userId();
		return userId.isPresent() ? userId.getAsLong() : null;
	}

	// ------------------------------------------------------------------
	// History
	// ------------------------------------------------------------------

	@Override
	public List<TestQuestionVersion> getVersions(Long testId, Long questionId) {
		Long tenantId = executionContext.tenantId();
		requireTest(tenantId, testId);
		requireQuestionOfTest(tenantId, testId, questionId);
		return questionRepository.findVersions(tenantId, questionId);
	}

	@Override
	public List<TestEvent> getEvents(Long testId, Long questionId) {
		Long tenantId = executionContext.tenantId();
		requireTest(tenantId, testId);
		if (questionId == null) {
			return eventRepository.findByTenantIdAndTestId(tenantId, testId);
		}
		requireQuestionOfTest(tenantId, testId, questionId);
		return eventRepository.findByTenantIdAndTestIdAndQuestionId(tenantId, testId, questionId);
	}

	@Override
	public List<GenerationRun> getGenerationRuns(Long testId) {
		Long tenantId = executionContext.tenantId();
		requireTest(tenantId, testId);
		return generationRunRepository.findByTenantIdAndTestId(tenantId, testId);
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private ReviewContext loadForAction(Long testId, Long questionId, Long expectedCurrentVersionId) {
		if (expectedCurrentVersionId == null) {
			throw new IllegalArgumentException("expectedCurrentVersionId is required");
		}
		Long tenantId = executionContext.tenantId();
		Test test = requireTest(tenantId, testId);
		// Fail fast before any LLM call; applyNewVersion checks again under a row lock.
		if (test.getStatus() == null || !test.getStatus().isReviewable()) {
			throw new ReviewConflictException(
					"Test " + testId + " cannot be reviewed in status " + test.getStatus() + ".");
		}
		TestQuestion question = requireQuestionOfTest(tenantId, testId, questionId);
		if (!expectedCurrentVersionId.equals(question.getCurrentVersionId())) {
			throw new ReviewConflictException("Question " + questionId + " is now at version id "
					+ question.getCurrentVersionId() + ", not " + expectedCurrentVersionId
					+ ". Reload it and review the current version.");
		}
		var userId = executionContext.userId();
		return new ReviewContext(tenantId, userId.isPresent() ? userId.getAsLong() : null, test, question);
	}

	private Test requireTest(Long tenantId, Long testId) {
		return testRepository.findByTenantIdAndId(tenantId, testId)
				.orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
	}

	private TestQuestion requireQuestionOfTest(Long tenantId, Long testId, Long questionId) {
		TestQuestion question = questionRepository.findByTenantIdAndId(tenantId, questionId)
				.orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
		if (!testId.equals(question.getTestId())) {
			throw new ResourceNotFoundException("Question " + questionId + " does not belong to test " + testId);
		}
		return question;
	}

	private List<TestQuestion> otherQuestions(ReviewContext ctx) {
		return questionRepository.findByTenantIdAndTestId(ctx.tenantId(), ctx.test().getId()).stream()
				.filter(q -> !q.getId().equals(ctx.question().getId())).toList();
	}

	private RegenerationFailedException regenerationFailed(ReviewContext ctx, String comment, String reason) {
		tx.recordQuestionEvent(ctx.question(), TestEventType.REGENERATION_FAILED, ctx.actorUserId(), comment,
				Map.of("reason", reason));
		log.warn("Review: test {} question {} - regeneration failed: {}", ctx.test().getId(),
				ctx.question().getId(), reason);
		return new RegenerationFailedException(
				"Regeneration of question " + ctx.question().getId() + " failed: " + reason + " Nothing was changed.");
	}
}
