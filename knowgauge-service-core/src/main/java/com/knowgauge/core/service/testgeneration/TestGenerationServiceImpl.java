package com.knowgauge.core.service.testgeneration;

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
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.core.service.retrieving.RetrievingService;
import com.knowgauge.core.service.testgeneration.prompt.TestPromptBuilder;
import com.knowgauge.core.service.testgeneration.validation.TestDraftValidator;
import com.knowgauge.core.service.testgeneration.validation.TestQuestionValidator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TestGenerationServiceImpl implements TestGenerationService {

	private final RetrievingService retrievingService;

	private final TestPromptBuilder promptBuilder;
	private final LlmTestGenerationService llmTestGenerationService;
	private final TestQuestionValidator testQuestionValidator;
	private final TestDraftValidator testDraftValidator;
	private final TestGenerationTransactionalServiceImpl tx;
	private final TestRepository testRepository;
	private final TestQuestionRepository testQuestionRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final ExecutionContext executionContext;
	private final TestGenerationDefaultsProperties defaults;

	public TestGenerationServiceImpl(RetrievingService retrievingService, TestPromptBuilder promptBuilder,
			LlmTestGenerationService llmTestGenerationService, TestQuestionValidator testQuestionValidator,
			TestDraftValidator testDraftValidator, TestGenerationTransactionalServiceImpl tx,
			TestRepository testRepository, TestQuestionRepository testQuestionRepository,
			DocumentChunkRepository documentChunkRepository, ExecutionContext executionContext,
			TestGenerationDefaultsProperties defaults) {
		this.retrievingService = retrievingService;
		this.promptBuilder = promptBuilder;
		this.llmTestGenerationService = llmTestGenerationService;
		this.testQuestionValidator = testQuestionValidator;
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

			// 2) Select embeddings based on topis IDs, document IDs, chunk limit, coverage
			// mode and avoiding repeats
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
				throw new IllegalStateException("No chunks could be loaded for retrieved embeddings (tenantId="
						+ tenantId + ", chunkIds=" + chunkIds.size() + ").");
			}

			tx.setUsedChunks(tenantId, test.getId(), chunkIds);
			log.info("    Test generation {} - Used chunks [{}] persisted for test {}", test.getId(), chunkIds,
					test.getId());

			// 3-6) Generate questions in batches
			int batchCount = defaults.getQuestionGenerationBatchSize();
			int totalQuestions = test.getQuestionCount();
			int generatedCount = 0;
			int batchIndex = 0;

			while (generatedCount < totalQuestions) {
				int batchSize = Math.min(batchCount, totalQuestions - generatedCount);
				int currentBatchGeneratedCount = generateTestQuestionBatch(++batchIndex, tenantId, test, chunks, embeddings, batchSize, generatedCount);
				generatedCount += currentBatchGeneratedCount;
				log.info("    Test generation {} - Batch No. {} - Progress: {}/{} questions generated", test.getId(),
						batchIndex, generatedCount, totalQuestions);
			}

			// 7) Mark test GENERATED
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

	private int generateTestQuestionBatch(int batchIndex, Long tenantId, Test test, List<DocumentChunk> chunks,
			List<ChunkEmbedding> embeddings, int batchSize, int generatedCount) {
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

				// 5) Validate questions
				List<TestQuestion> validatedTestQuestions = testQuestionValidator.validateAndNormalize(
						generatedTestQuestions, test, embeddings, generatedCount);
				log.info("    Test generation {} - Batch No. {} - Validated and normalized {} questions in batch.",
						test.getId(), batchIndex, generatedTestQuestions.size());

				// 6) Persist questions
				tx.persistTestQuestions(tenantId, test.getId(), validatedTestQuestions, embeddings);
				log.info("    Test generation {} - Batch No. {} - Persisted {} validated questions", test.getId(),
						batchIndex, validatedTestQuestions.size());

				return validatedTestQuestions.size();

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
		int percentage = defaults.getMinMultipleCorrectPercentage() != null
				? defaults.getMinMultipleCorrectPercentage()
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
