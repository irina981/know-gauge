package com.knowgauge.core.service.testgeneration;

import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.GenerationRunPurpose;
import com.knowgauge.core.model.enums.VersionOrigin;
import com.knowgauge.core.port.testgeneration.LlmGenerationResult;
import com.knowgauge.core.port.testgeneration.LlmIncorrectOptionsVerificationService;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.core.port.testgeneration.LlmVerificationResult;
import com.knowgauge.core.service.testgeneration.GenerationRunRecorder.RunContext;
import com.knowgauge.core.service.testgeneration.prompt.TestGenerationPromptBuilder;
import com.knowgauge.core.service.testgeneration.prompt.VerificationPromptBuilder;
import com.knowgauge.core.service.testgeneration.validation.PostLlmFinalValidator;
import com.knowgauge.core.service.testgeneration.validation.PreLlmPreflightValidator;

import lombok.extern.slf4j.Slf4j;

/**
 * Generates one batch of validated, verified draft questions:
 * prompt → LLM → pre-validate → verify distractors (flag only) → post-validate.
 *
 * <p>
 * Shared by initial test generation and single-question regeneration (ADR001).
 * Every LLM call is recorded as a {@link GenerationRun}. Drafts carry their
 * generation and verification run ids and their origin. Not transactional:
 * nothing but the run records is persisted here.
 */
@Slf4j
@Component
public class QuestionBatchGenerator {

	private final TestGenerationPromptBuilder promptBuilder;
	private final VerificationPromptBuilder verificationPromptBuilder;
	private final LlmTestGenerationService llmTestGenerationService;
	private final LlmIncorrectOptionsVerificationService verificationService;
	private final PreLlmPreflightValidator preLlmValidator;
	private final PostLlmFinalValidator postLlmValidator;
	private final GenerationRunRecorder runRecorder;
	private final TestGenerationDefaultsProperties defaults;

	public QuestionBatchGenerator(TestGenerationPromptBuilder promptBuilder,
			VerificationPromptBuilder verificationPromptBuilder, LlmTestGenerationService llmTestGenerationService,
			LlmIncorrectOptionsVerificationService verificationService, PreLlmPreflightValidator preLlmValidator,
			PostLlmFinalValidator postLlmValidator, GenerationRunRecorder runRecorder,
			TestGenerationDefaultsProperties defaults) {
		this.promptBuilder = promptBuilder;
		this.verificationPromptBuilder = verificationPromptBuilder;
		this.llmTestGenerationService = llmTestGenerationService;
		this.verificationService = verificationService;
		this.preLlmValidator = preLlmValidator;
		this.postLlmValidator = postLlmValidator;
		this.runRecorder = runRecorder;
		this.defaults = defaults;
	}

	/**
	 * @param test                     persisted test (id, tenant, config)
	 * @param chunks                   grounding context
	 * @param chunkIds                 ids of the grounding chunks (recorded, and
	 *                                 allowed as citations)
	 * @param batchSize                questions requested
	 * @param minMultipleCorrectOverride null to derive from the defaults
	 *                                 percentage, otherwise the exact number of
	 *                                 multi-correct questions required
	 * @param startIndex               first question index assigned to valid
	 *                                 questions
	 * @param batchIndex               batch number (for runs and logs)
	 * @param purpose                  TEST_GENERATION or REGENERATION
	 * @param questionId               slot being regenerated (REGENERATION only)
	 * @param avoidQuestionTexts       existing questions the LLM must not repeat
	 */
	public record BatchRequest(Test test, List<DocumentChunk> chunks, List<Long> chunkIds, int batchSize,
			Integer minMultipleCorrectOverride, int startIndex, int batchIndex, GenerationRunPurpose purpose,
			Long questionId, List<String> avoidQuestionTexts) {
	}

	/**
	 * @return validated draft questions (possibly empty). Flagged options are kept
	 *         for review.
	 * @throws LlmResponseParsingException when the output stays too long even for
	 *                                     one question, or cannot be parsed
	 */
	public List<TestQuestion> generateBatch(BatchRequest req) {
		Test test = req.test();
		int currentSize = req.batchSize();
		int attempt = 0;

		while (true) {
			attempt++;
			Test batchTest = createBatchTestCopy(test, currentSize, req.minMultipleCorrectOverride());
			String templateId = promptBuilder.resolveTemplateId(batchTest);
			RunContext genCtx = new RunContext(test, req.questionId(), req.purpose(), req.batchIndex(), templateId,
					req.chunkIds());
			String prompt = promptBuilder.buildPrompt(batchTest, req.chunks(), req.avoidQuestionTexts());
			log.debug("    Test generation {} - Batch No. {} - Prompt for {} questions: {}", test.getId(),
					req.batchIndex(), currentSize, prompt);

			LlmGenerationResult generation;
			try {
				generation = llmTestGenerationService.generate(prompt, batchTest);
			} catch (LlmResponseParsingException ex) {
				runRecorder.failure(genCtx, null, ex.getReason() + ": " + ex.getMessage());
				if (ex.getReason() == LlmResponseParsingException.Reason.LENGTH && currentSize > 1) {
					currentSize--;
					log.warn(
							"    Test generation {} - Batch No. {} - LLM output exceeded max tokens, reducing batch size to {} and retrying (attempt {})",
							test.getId(), req.batchIndex(), currentSize, attempt + 1);
					continue;
				}
				throw ex;
			} catch (RuntimeException ex) {
				runRecorder.failure(genCtx, null, ex.getMessage());
				throw ex;
			}

			GenerationRun genRun = runRecorder.success(genCtx, generation.call());
			List<TestQuestion> generated = generation.questions() == null ? List.of() : generation.questions();
			log.info("    Test generation {} - Batch No. {} - Generated {} questions (run {}).", test.getId(),
					req.batchIndex(), generated.size(), genRun.getId());
			tagDrafts(generated, test, genRun.getId(), originFor(req.purpose()));

			// Pre-validation filters unsafe questions before paying for verification
			List<TestQuestion> preValidated = preLlmValidator.validateAndNormalize(generated, batchTest,
					req.chunkIds(), req.startIndex());

			verify(preValidated, batchTest, req);

			List<TestQuestion> validated = postLlmValidator.validateAndNormalize(preValidated, batchTest,
					req.chunkIds(), req.startIndex());
			if (validated.isEmpty()) {
				log.warn("    Test generation {} - Batch No. {} - All {} generated questions were rejected by validation.",
						test.getId(), req.batchIndex(), generated.size());
			}
			return validated;
		}
	}

	/**
	 * Flags distractors that the verifier judges true. Best effort: a failed
	 * verification is recorded and generation continues with unverified drafts.
	 */
	private void verify(List<TestQuestion> questions, Test batchTest, BatchRequest req) {
		if (questions.isEmpty()) {
			return;
		}
		RunContext ctx = new RunContext(req.test(), req.questionId(), GenerationRunPurpose.VERIFICATION,
				req.batchIndex(), verificationPromptBuilder.getTemplateId(), req.chunkIds());
		LlmVerificationResult result;
		try {
			String prompt = verificationPromptBuilder.buildPrompt(questions, batchTest);
			result = verificationService.verify(prompt, batchTest, questions);
		} catch (RuntimeException ex) {
			result = new LlmVerificationResult(0, null, ex.getMessage());
		}

		GenerationRun run = result.succeeded() ? runRecorder.success(ctx, result.call())
				: runRecorder.failure(ctx, result.call(), result.errorMessage());
		for (TestQuestion q : questions) {
			q.getCurrentVersion().setVerificationRunId(run.getId());
		}
		if (!result.succeeded()) {
			log.warn("    Test generation {} - Batch No. {} - Verification failed (run {}), continuing unverified: {}",
					req.test().getId(), req.batchIndex(), run.getId(), result.errorMessage());
		} else if (result.flaggedCount() > 0) {
			log.info("    Test generation {} - Batch No. {} - Verifier flagged {} options (run {}).", req.test().getId(),
					req.batchIndex(), result.flaggedCount(), run.getId());
		}
	}

	private void tagDrafts(List<TestQuestion> questions, Test test, Long generationRunId, VersionOrigin origin) {
		for (TestQuestion q : questions) {
			if (q == null) {
				continue;
			}
			q.setTenantId(test.getTenantId());
			q.setTestId(test.getId());
			if (q.getCurrentVersion() == null) {
				q.setCurrentVersion(new TestQuestionVersion());
			}
			TestQuestionVersion v = q.getCurrentVersion();
			v.setTenantId(test.getTenantId());
			v.setGenerationRunId(generationRunId);
			v.setOrigin(origin);
		}
	}

	private static VersionOrigin originFor(GenerationRunPurpose purpose) {
		return purpose == GenerationRunPurpose.REGENERATION ? VersionOrigin.REGENERATED : VersionOrigin.GENERATED;
	}

	private Test createBatchTestCopy(Test test, int batchSize, Integer minMultiOverride) {
		int minMulti = minMultiOverride != null ? minMultiOverride
				: computeMinMultipleCorrectQuestionsCount(batchSize, test.getAnswerCardinality());
		return Test.builder().tenantId(test.getTenantId()).id(test.getId()).difficulty(test.getDifficulty())
				.avoidRepeats(test.getAvoidRepeats()).coverageMode(test.getCoverageMode()).questionCount(batchSize)
				.answerCardinality(test.getAnswerCardinality()).minMultipleCorrectQuestionsCount(minMulti)
				.language(test.getLanguage()).generationModel(test.getGenerationModel())
				.promptTemplateId(test.getPromptTemplateId()).generationParams(test.getGenerationParams()).build();
	}

	/**
	 * Number of questions that must be multi-correct in a batch of
	 * {@code questionCount}.
	 */
	public int computeMinMultipleCorrectQuestionsCount(Integer questionCount, AnswerCardinality answerCardinality) {
		if (questionCount == null || questionCount <= 0 || answerCardinality != AnswerCardinality.MULTIPLE_CORRECT) {
			return 0;
		}
		int percentage = defaults.getMinMultipleCorrectPercentage() != null ? defaults.getMinMultipleCorrectPercentage()
				: 20;
		return (int) Math.max(1, Math.ceil(questionCount * percentage / 100.0));
	}
}
