package com.knowgauge.core.service.testgeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.LlmResponseParsingException;
import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.port.testgeneration.LlmIncorrectOptionsVerificationService;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.core.service.retrieving.RetrievingService;
import com.knowgauge.core.service.testgeneration.prompt.TestPromptBuilder;
import com.knowgauge.core.service.testgeneration.validation.PostLlmFinalValidator;
import com.knowgauge.core.service.testgeneration.validation.PreLlmPreflightValidator;
import com.knowgauge.core.service.testgeneration.validation.TestDraftValidator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TestGenerationServiceImpl implements TestGenerationService {

	private final RetrievingService retrievingService;

	private final TestPromptBuilder promptBuilder;
	private final LlmTestGenerationService llmTestGenerationService;
	private final LlmIncorrectOptionsVerificationService llmIncorrectOptionsVerificationService;
	private final PostLlmFinalValidator postLlmFinalValidator;
	private final PreLlmPreflightValidator preLlmPreflightValidator;
	private final TestDraftValidator testDraftValidator;
	private final TestGenerationTransactionalServiceImpl tx;
	private final TestRepository testRepository;
	private final TestQuestionRepository testQuestionRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final ExecutionContext executionContext;
	private final TestGenerationDefaultsProperties defaults;

	public TestGenerationServiceImpl(RetrievingService retrievingService, TestPromptBuilder promptBuilder,
			LlmTestGenerationService llmTestGenerationService,
			LlmIncorrectOptionsVerificationService llmIncorrectOptionsVerificationService,
			PreLlmPreflightValidator preLlmPreflightValidator, PostLlmFinalValidator postLlmFinalValidator,
			TestDraftValidator testDraftValidator, TestGenerationTransactionalServiceImpl tx,
			TestRepository testRepository, TestQuestionRepository testQuestionRepository,
			DocumentChunkRepository documentChunkRepository, ExecutionContext executionContext,
			TestGenerationDefaultsProperties defaults) {
		this.retrievingService = retrievingService;
		this.promptBuilder = promptBuilder;
		this.llmTestGenerationService = llmTestGenerationService;
		this.llmIncorrectOptionsVerificationService = llmIncorrectOptionsVerificationService;
		this.preLlmPreflightValidator = preLlmPreflightValidator;
		this.postLlmFinalValidator = postLlmFinalValidator;
		this.testDraftValidator = testDraftValidator;
		this.tx = tx;
		this.testRepository = testRepository;
		this.testQuestionRepository = testQuestionRepository;
		this.documentChunkRepository = documentChunkRepository;
		this.executionContext = executionContext;
		this.defaults = defaults;
	}

	@Override
	public Test generate(Test testDraft) {
		Long tenantId = executionContext.tenantId();
		applyDefaults(testDraft);
		testDraft.setTenantId(tenantId);
		log.info("*** Test generation - Started for tenantId={}, topicIds={}, documentIds={}. questionCount={}",
				tenantId, testDraft.getTopicIds(), testDraft.getDocumentIds(), testDraft.getQuestionCount());

		// 0) Validate test and expand:
		// - topic IDs - with all the descendants of the selected topics
		// - document IDs - with all the documents for all expanded topic IDs
		testDraft = testDraftValidator.validateAndExpandTopicsAndDocuments(tenantId, testDraft);
		List<Long> topicIds = testDraft.getTopicIds();
		log.info("    Test generation - Expanded topic IDs: [{}],", topicIds);
		List<Long> documentIds = testDraft.getDocumentIds();
		log.info("    Test generation - Expanded document IDs: [{}],", documentIds);

		// 1) Persist test and mark it GENERATING
		Test test = tx.persistTest(tenantId, testDraft);
		test.setMinMultipleCorrectQuestionsCount(
				computeMinMultipleCorrectQuestionsCount(test.getQuestionCount(), test.getAnswerCardinality()));
		log.info("    Test generation {} - Test persisted", test.getId());

		try {

			// 2) Retrieve and load chunks
			ChunksContext chunksContext = retrieveAndLoadChunks(tenantId, test, documentIds, topicIds);

			// 3-9) Generate and validate questions in batches
			List<TestQuestion> allValidatedQuestions = generateAllTestQuestionBatches(tenantId, test,
					chunksContext.chunks(), chunksContext.embeddings());

			// 10) Persist all validated questions at once
			tx.persistTestQuestions(tenantId, test.getId(), allValidatedQuestions, chunksContext.embeddings());
			log.info("    Test generation {} - Persisted {} validated questions", test.getId(),
					allValidatedQuestions.size());

			// 11) Mark test GENERATED
			Test ready = tx.markTestGenerated(tenantId, test.getId());
			log.info("    Test generation {} - Test marked GENERATED.", test.getId());

			log.info("*** Test generation {} - Completed", ready.getId());

			return ready;

		} catch (Exception ex) {

			// Mark test FAILED
			tx.markTestFailed(tenantId, test.getId(), ex.getMessage());
			log.info("    Test generation {} - Test marked FAILED, with message: {}.", test.getId(), ex.getMessage());

			throw new RuntimeException("Test generation failed for test " + test.getId(), ex);
		}
	}

	/**
	 * Retrieves embeddings and loads corresponding document chunks for test generation.
	 *
	 * @param tenantId    the tenant ID
	 * @param test        the test being generated
	 * @param documentIds the document IDs to retrieve from
	 * @param topicIds    the topic IDs for error messages
	 * @return context containing chunks and embeddings
	 */
	private ChunksContext retrieveAndLoadChunks(Long tenantId, Test test, List<Long> documentIds,
			List<Long> topicIds) {
		List<ChunkEmbedding> embeddings = retrievingService.retrieveTop(tenantId, documentIds,
				recommendedChunkLimit(test), test.getCoverageMode(), Boolean.TRUE.equals(test.getAvoidRepeats()));
		if (embeddings == null || embeddings.isEmpty()) {
			throw new IllegalStateException("No relevant context chunks found (tenantId=" + tenantId + ", topicIds="
					+ topicIds + ", documentIds=" + documentIds + ").");
		}
		log.info("    Test generation {} - Retrieved {} embeddings from vector store", test.getId(),
				embeddings.size());

		List<Long> chunkIds = embeddings.stream().map(ChunkEmbedding::getChunkId).toList();
		log.info("    Test generation {} - Chunk IDs that will be used for test generation: [{}]", test.getId(),
				chunkIds);

		List<DocumentChunk> chunks = documentChunkRepository.findByTenantIdAndIdIn(tenantId, chunkIds);
		if (chunks == null || chunks.isEmpty()) {
			throw new IllegalStateException("No chunks could be loaded for retrieved embeddings (tenantId=" + tenantId
					+ ", chunkIds=" + chunkIds.size() + ").");
		}

		tx.setUsedChunks(tenantId, test.getId(), chunkIds);
		log.info("    Test generation {} - Used chunks [{}] persisted for test {}", test.getId(), chunkIds,
				test.getId());

		return new ChunksContext(chunks, embeddings);
	}

	/**
	 * Generates all question batches for the test with zero-progress tracking.
	 *
	 * @param tenantId   the tenant ID
	 * @param test       the test being generated
	 * @param chunks     the document chunks for context
	 * @param embeddings the chunk embeddings
	 * @return list of all validated questions
	 */
	private List<TestQuestion> generateAllTestQuestionBatches(Long tenantId, Test test, List<DocumentChunk> chunks,
			List<ChunkEmbedding> embeddings) {
		int batchCount = defaults.getQuestionGenerationBatchSize();
		int totalQuestions = test.getQuestionCount();
		int generatedCount = 0;
		int batchIndex = 0;
		List<TestQuestion> allValidatedQuestions = new ArrayList<>();
		int consecutiveZeroProgressBatches = 0;
		int maxConsecutiveZeroProgress = getMaxConsecutiveZeroProgress();

		while (generatedCount < totalQuestions) {
			int batchSize = Math.min(batchCount, totalQuestions - generatedCount);
			List<TestQuestion> batchQuestions = generateTestQuestionBatch(++batchIndex, tenantId, test, chunks,
					embeddings, batchSize, generatedCount);

			if (batchQuestions.isEmpty()) {
				consecutiveZeroProgressBatches++;
				checkZeroProgressThreshold(test, batchIndex, consecutiveZeroProgressBatches, maxConsecutiveZeroProgress,
						generatedCount, totalQuestions);
			} else {
				consecutiveZeroProgressBatches = 0; // Reset on success
			}

			allValidatedQuestions.addAll(batchQuestions);
			generatedCount += batchQuestions.size();
			log.info("    Test generation {} - Batch No. {} - Progress: {}/{} questions generated", test.getId(),
					batchIndex, generatedCount, totalQuestions);
		}

		return allValidatedQuestions;
	}

	/**
	 * Checks if zero-progress threshold has been exceeded and throws exception if so.
	 */
	private void checkZeroProgressThreshold(Test test, int batchIndex, int consecutiveZeroProgressBatches,
			int maxConsecutiveZeroProgress, int generatedCount, int totalQuestions) {
		log.warn(
				"    Test generation {} - Batch No. {} returned 0 valid questions. Consecutive zero-progress batches: {}/{}",
				test.getId(), batchIndex, consecutiveZeroProgressBatches, maxConsecutiveZeroProgress);

		if (consecutiveZeroProgressBatches >= maxConsecutiveZeroProgress) {
			throw new IllegalStateException("Test generation failed for test " + test.getId() + ": "
					+ consecutiveZeroProgressBatches + " consecutive batches returned 0 valid questions. Generated "
					+ generatedCount + "/" + totalQuestions
					+ " questions. Check validation rules, LLM prompt, or context quality.");
		}
	}

	/**
	 * Returns the configured max consecutive zero-progress batches limit.
	 */
	private int getMaxConsecutiveZeroProgress() {
		return defaults.getMaxConsecutiveZeroProgressBatches() != null
				? defaults.getMaxConsecutiveZeroProgressBatches()
				: 3;
	}

	/**
	 * Record to encapsulate chunks and embeddings context.
	 */
	private record ChunksContext(List<DocumentChunk> chunks, List<ChunkEmbedding> embeddings) {
	}

	private List<TestQuestion> generateTestQuestionBatch(int batchIndex, Long tenantId, Test test,
			List<DocumentChunk> chunks, List<ChunkEmbedding> embeddings, int batchSize, int generatedCount) {
		int currentBatchSize = batchSize;
		int maxRetries = batchSize - 1; // Maximum retries equals batch size minus 1 (down to 1 question)
		int retryCount = 0;

		while (retryCount <= maxRetries) {
			try {
				// 3) Build prompt for this batch
				Test batchTest = createBatchTestCopy(test, currentBatchSize);
				String prompt = promptBuilder.buildPrompt(batchTest, chunks);
				log.debug("    Test generation {} - Batch No. {} - Built prompt for batch of {} questions: {}",
						test.getId(), batchIndex, currentBatchSize, prompt);

				// 4) Generate questions for this batch
				List<TestQuestion> generatedTestQuestions = llmTestGenerationService.generate(prompt, batchTest);
				log.info("    Test generation {} - Batch No. {} - Generated {} questions in batch.", test.getId(),
						batchIndex, generatedTestQuestions.size());

				// 5-8) Process multi-correct questions if needed
				List<TestQuestion> questionsForPostValidation = processMultiCorrectQuestions(generatedTestQuestions, test,
						embeddings, generatedCount, batchIndex);

				// 9) Post-LLM validation (full)
				List<TestQuestion> validatedTestQuestions = postLlmFinalValidator
						.validateAndNormalize(questionsForPostValidation, test, embeddings, generatedCount);
				log.info("    Test generation {} - Batch No. {} - Post-validated and normalized {} questions in batch.",
						test.getId(), batchIndex, validatedTestQuestions.size());

				if (validatedTestQuestions.isEmpty()) {
					log.warn(
							"    Test generation {} - Batch No. {} - Validation returned 0 questions (generated: {}, pre-validated: {}, post-validated: 0). All questions were filtered out by validation rules.",
							test.getId(), batchIndex, generatedTestQuestions.size(), questionsForPostValidation.size());
				}

				return validatedTestQuestions;

			} catch (LlmResponseParsingException ex) {
				if (ex.getReason() == LlmResponseParsingException.Reason.LENGTH) {
					if (currentBatchSize > 1) {
						retryCount++;
						currentBatchSize--;
						log.warn(
								"    Test generation {} - Batch No. {} - LLM output exceeded max tokens, reducing batch size to {} and retrying (attempt {}/{})",
								test.getId(), batchIndex, currentBatchSize, retryCount, maxRetries + 1);
					} else {
						log.error(
								"    Test generation {} - Batch No. {} - LLM output exceeded max tokens even with batch size 1, cannot retry further",
								test.getId(), batchIndex);
						throw ex;
					}
				} else {
					// For non-LENGTH reasons, rethrow immediately
					throw ex;
				}
			}
		}

		// This should never be reached, but add for completeness
		throw new IllegalStateException(
				"Failed to generate batch " + batchIndex + " for test " + test.getId() + " after all retries");
	}

	/**
	 * Processes generated questions - for MULTIPLE_CORRECT tests, splits, validates,
	 * verifies, and merges questions.
	 *
	 * @param generatedQuestions the questions generated by LLM
	 * @param test               the test being generated
	 * @param embeddings         the chunk embeddings
	 * @param generatedCount     the count of questions generated so far
	 * @param batchIndex         the current batch index for logging
	 * @return questions ready for post-validation
	 */
	private List<TestQuestion> processMultiCorrectQuestions(List<TestQuestion> generatedQuestions, Test test,
			List<ChunkEmbedding> embeddings, int generatedCount, int batchIndex) {
		if (test.getAnswerCardinality() != AnswerCardinality.MULTIPLE_CORRECT) {
			return generatedQuestions;
		}

		// 5) Split questions: multi-correct for preflight validation, single-correct passthrough
		List<TestQuestion> multiCorrectQuestions = generatedQuestions.stream()
				.filter(q -> q != null && q.getCorrectOptions() != null && q.getCorrectOptions().size() > 1).toList();
		List<TestQuestion> singleCorrectQuestions = generatedQuestions.stream()
				.filter(q -> q == null || q.getCorrectOptions() == null || q.getCorrectOptions().size() <= 1).toList();
		log.debug("    Test generation {} - Batch No. {} - Split questions: {} multi-correct, {} single-correct",
				test.getId(), batchIndex, multiCorrectQuestions.size(), singleCorrectQuestions.size());

		// 6) Pre-LLM validation (multi-correct only) to filter unsafe questions before verification
		List<TestQuestion> preValidatedMultiCorrectQuestions = preLlmPreflightValidator
				.validateAndNormalize(multiCorrectQuestions, test, embeddings, generatedCount);
		log.info("    Test generation {} - Batch No. {} - Pre-validated {} multi-correct questions.", test.getId(),
				batchIndex, preValidatedMultiCorrectQuestions.size());

		// 7) Verify and replace unsafe incorrect options for MULTIPLE_CORRECT questions
		int replacedCount = llmIncorrectOptionsVerificationService.verifyAndReplaceUnsafeOptions(
				preValidatedMultiCorrectQuestions, test);
		if (replacedCount > 0) {
			log.info("    Test generation {} - Batch No. {} - Verified and replaced {} unsafe incorrect options.",
					test.getId(), batchIndex, replacedCount);
		}

		// 8) Merge and shuffle pre-validated multi-correct and passthrough single-correct questions
		List<TestQuestion> merged = mergeAndShuffleQuestions(preValidatedMultiCorrectQuestions, singleCorrectQuestions,
				generatedCount);
		log.debug("    Test generation {} - Batch No. {} - Merged and shuffled {} questions for post-validation",
				test.getId(), batchIndex, merged.size());

		return merged;
	}

	/**
	 * Merges multi-correct and single-correct questions, shuffles them for random
	 * distribution, and reassigns question indexes sequentially.
	 *
	 * @param multiCorrectQuestions  the pre-validated multi-correct questions
	 * @param singleCorrectQuestions the passthrough single-correct questions
	 * @param startIndex             the starting index for question numbering
	 * @return merged and shuffled list with corrected question indexes
	 */
	private List<TestQuestion> mergeAndShuffleQuestions(List<TestQuestion> multiCorrectQuestions,
			List<TestQuestion> singleCorrectQuestions, int startIndex) {
		List<TestQuestion> merged = new ArrayList<>();
		merged.addAll(multiCorrectQuestions);
		merged.addAll(singleCorrectQuestions);

		// Shuffle to ensure random distribution of multi-correct and single-correct
		// questions
		Collections.shuffle(merged);

		// Reassign question indexes sequentially after shuffling
		int nextIndex = startIndex;
		for (TestQuestion question : merged) {
			question.setQuestionIndex(nextIndex++);
		}

		return merged;
	}

	private Test createBatchTestCopy(Test test, int batchSize) {
		Test batchTest = Test.builder().tenantId(test.getTenantId()).id(test.getId()).difficulty(test.getDifficulty())
				.avoidRepeats(test.getAvoidRepeats()).coverageMode(test.getCoverageMode()).questionCount(batchSize)
				.answerCardinality(test.getAnswerCardinality())
				.minMultipleCorrectQuestionsCount(
						computeMinMultipleCorrectQuestionsCount(batchSize, test.getAnswerCardinality()))
				.language(test.getLanguage()).generationModel(test.getGenerationModel())
				.promptTemplateId(test.getPromptTemplateId()).generationParams(test.getGenerationParams()).build();
		return batchTest;
	}

	private int recommendedChunkLimit(Test test) {
		return Math.max(defaults.getMinChunksPerTest(), test.getQuestionCount() * defaults.getChunksPerQuestion());
	}

	private void applyDefaults(Test test) {
		if (test.getDifficulty() == null) {
			test.setDifficulty(defaults.getDifficulty());
		}
		if (test.getAvoidRepeats() == null) {
			test.setAvoidRepeats(defaults.getAvoidRepeats());
		}
		if (test.getCoverageMode() == null) {
			test.setCoverageMode(defaults.getCoverageMode());
		}
		if (test.getQuestionCount() == null) {
			test.setQuestionCount(defaults.getQuestionCount());
		}
		if (test.getAnswerCardinality() == null) {
			test.setAnswerCardinality(defaults.getAnswerCardinality());
		}
		if (test.getMinMultipleCorrectQuestionsCount() == null) {
			test.setMinMultipleCorrectQuestionsCount(
					computeMinMultipleCorrectQuestionsCount(test.getQuestionCount(), test.getAnswerCardinality()));
		}
		if (test.getLanguage() == null) {
			test.setLanguage(defaults.getLanguage());
		}
		if (isBlank(test.getPromptTemplateId())) {
			test.setPromptTemplateId(defaults.getPromptTemplateId());
		}
		if (isBlank(test.getGenerationModel())) {
			test.setGenerationModel(defaults.getGenerationModel());
		}
	}

	private int computeMinMultipleCorrectQuestionsCount(Integer questionCount, AnswerCardinality answerCardinality) {
		if (questionCount == null || questionCount <= 0 || answerCardinality != AnswerCardinality.MULTIPLE_CORRECT) {
			return 0;
		}
		int percentage = defaults.getMinMultipleCorrectPercentage() != null ? defaults.getMinMultipleCorrectPercentage()
				: 20;
		return (int) Math.max(1, Math.ceil(questionCount * percentage / 100.0));
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	@Override
	public Optional<Test> getById(Long testId) {
		Long tenantId = executionContext.tenantId();
		return testRepository.findByTenantIdAndId(tenantId, testId);
	}

	@Override
	public List<Test> getAll() {
		Long tenantId = executionContext.tenantId();
		return testRepository.findByTenantId(tenantId);
	}

	@Override
	public List<Test> getAllByStatus(TestStatus status) {
		Long tenantId = executionContext.tenantId();
		return testRepository.findByTenantIdAndStatus(tenantId, status);

	}

	@Override
	public Optional<List<TestQuestion>> getQuestionsByTestId(Long testId) {
		Long tenantId = executionContext.tenantId();
		if (testRepository.findByTenantIdAndId(tenantId, testId).isEmpty()) {
			return Optional.empty();
		}

		List<TestQuestion> questions = testQuestionRepository.findByTestId(testId).stream().sorted((left, right) -> {
			Integer leftIndex = left.getQuestionIndex();
			Integer rightIndex = right.getQuestionIndex();
			if (leftIndex == null && rightIndex == null) {
				return 0;
			}
			if (leftIndex == null) {
				return 1;
			}
			if (rightIndex == null) {
				return -1;
			}
			return Integer.compare(leftIndex, rightIndex);
		}).toList();

		return Optional.of(questions);
	}

	@Override
	public void deleteById(Long testId) {
		Long tenantId = executionContext.tenantId();
		testRepository.deleteByTenantIdAndId(tenantId, testId);
	}
}
