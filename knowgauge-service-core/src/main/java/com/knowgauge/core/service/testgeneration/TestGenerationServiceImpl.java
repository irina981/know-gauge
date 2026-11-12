package com.knowgauge.core.service.testgeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.GenerationRunPurpose;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.service.retrieving.RetrievingService;
import com.knowgauge.core.service.testgeneration.QuestionBatchGenerator.BatchRequest;
import com.knowgauge.core.service.testgeneration.validation.TestDraftValidator;

import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates test generation (not transactional; see
 * {@link TestGenerationTransactionalServiceImpl}).
 *
 * <p>
 * Questions whose distractors the verifier flagged are kept with
 * {@code NEEDS_REVIEW} status, and the test ends in {@code REQUIRES_REVIEW}
 * (ADR001). Every LLM call is recorded as a generation run, every state change
 * as a test event.
 */
@Service
@Slf4j
public class TestGenerationServiceImpl implements TestGenerationService {

	private final RetrievingService retrievingService;
	private final QuestionBatchGenerator batchGenerator;
	private final TestDraftValidator testDraftValidator;
	private final TestGenerationTransactionalServiceImpl tx;
	private final TestRepository testRepository;
	private final TestQuestionRepository testQuestionRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final ExecutionContext executionContext;
	private final TestGenerationDefaultsProperties defaults;

	public TestGenerationServiceImpl(RetrievingService retrievingService, QuestionBatchGenerator batchGenerator,
			TestDraftValidator testDraftValidator, TestGenerationTransactionalServiceImpl tx,
			TestRepository testRepository, TestQuestionRepository testQuestionRepository,
			DocumentChunkRepository documentChunkRepository, ExecutionContext executionContext,
			TestGenerationDefaultsProperties defaults) {
		this.retrievingService = retrievingService;
		this.batchGenerator = batchGenerator;
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
		Long actorUserId = currentUserId();
		applyDefaults(testDraft);
		testDraft.setTenantId(tenantId);
		log.info("*** Test generation - Started for tenantId={}, topicIds={}, documentIds={}. questionCount={}",
				tenantId, testDraft.getTopicIds(), testDraft.getDocumentIds(), testDraft.getQuestionCount());

		// 0) Validate test and expand topic and document scope
		testDraft = testDraftValidator.validateAndExpandTopicsAndDocuments(tenantId, testDraft);
		List<Long> topicIds = testDraft.getTopicIds();
		List<Long> documentIds = testDraft.getDocumentIds();
		log.info("    Test generation - Expanded topic IDs: [{}], document IDs: [{}]", topicIds, documentIds);

		// 1) Persist test (CREATED) + TEST_CREATED event
		Test test = tx.persistTest(tenantId, testDraft, actorUserId);
		test.setMinMultipleCorrectQuestionsCount(batchGenerator
				.computeMinMultipleCorrectQuestionsCount(test.getQuestionCount(), test.getAnswerCardinality()));
		log.info("    Test generation {} - Test persisted", test.getId());

		try {
			// 2) Retrieve and load chunks
			ChunksContext ctx = retrieveAndLoadChunks(tenantId, test, documentIds, topicIds);

			// 3) Generate, validate and verify questions in batches
			List<TestQuestion> questions = generateAllBatches(test, ctx);

			// 4) Persist all questions (slots + version 1) and their history at once
			List<TestQuestion> saved = tx.persistGeneratedQuestions(tenantId, test.getId(), questions,
					ctx.chunkIds());
			long needingReview = saved.stream().filter(TestQuestion::needsReview).count();
			log.info("    Test generation {} - Persisted {} questions ({} need review)", test.getId(), saved.size(),
					needingReview);

			// 5) GENERATED, or REQUIRES_REVIEW when any question has flagged options
			Test ready = needingReview > 0 ? tx.markTestRequiresReview(tenantId, test.getId(), (int) needingReview)
					: tx.markTestGenerated(tenantId, test.getId());
			log.info("*** Test generation {} - Completed with status {}", ready.getId(), ready.getStatus());
			return ready;

		} catch (Exception ex) {
			tx.markTestFailed(tenantId, test.getId(), ex.getMessage());
			log.info("    Test generation {} - Test marked FAILED, with message: {}.", test.getId(), ex.getMessage());
			throw new RuntimeException("Test generation failed for test " + test.getId(), ex);
		}
	}

	private record ChunksContext(List<DocumentChunk> chunks, List<Long> chunkIds) {
	}

	private ChunksContext retrieveAndLoadChunks(Long tenantId, Test test, List<Long> documentIds, List<Long> topicIds) {
		List<ChunkEmbedding> embeddings = retrievingService.retrieveTop(tenantId, documentIds,
				recommendedChunkLimit(test), test.getCoverageMode(), Boolean.TRUE.equals(test.getAvoidRepeats()));
		if (embeddings == null || embeddings.isEmpty()) {
			throw new IllegalStateException("No relevant context chunks found (tenantId=" + tenantId + ", topicIds="
					+ topicIds + ", documentIds=" + documentIds + ").");
		}
		List<Long> chunkIds = embeddings.stream().map(ChunkEmbedding::getChunkId).distinct().toList();
		log.info("    Test generation {} - Retrieved {} chunks: [{}]", test.getId(), chunkIds.size(), chunkIds);

		List<DocumentChunk> chunks = documentChunkRepository.findByTenantIdAndIdIn(tenantId, chunkIds);
		if (chunks == null || chunks.isEmpty()) {
			throw new IllegalStateException("No chunks could be loaded for retrieved embeddings (tenantId=" + tenantId
					+ ", chunkIds=" + chunkIds.size() + ").");
		}

		tx.setUsedChunks(tenantId, test.getId(), chunkIds);
		return new ChunksContext(chunks, chunkIds);
	}

	/**
	 * Generates batches until the requested count is reached, failing after too
	 * many consecutive batches without any valid question.
	 */
	private List<TestQuestion> generateAllBatches(Test test, ChunksContext ctx) {
		int batchSize = defaults.getQuestionGenerationBatchSize();
		int totalQuestions = test.getQuestionCount();
		int maxZeroProgress = defaults.getMaxConsecutiveZeroProgressBatches() != null
				? defaults.getMaxConsecutiveZeroProgressBatches()
				: 3;
		List<TestQuestion> all = new ArrayList<>();
		int batchIndex = 0;
		int zeroProgress = 0;

		while (all.size() < totalQuestions) {
			int size = Math.min(batchSize, totalQuestions - all.size());
			List<TestQuestion> batch = batchGenerator.generateBatch(new BatchRequest(test, ctx.chunks(),
					ctx.chunkIds(), size, null, all.size(), ++batchIndex, GenerationRunPurpose.TEST_GENERATION, null,
					List.of()));

			if (batch.isEmpty()) {
				zeroProgress++;
				log.warn("    Test generation {} - Batch No. {} returned 0 valid questions ({}/{} consecutive).",
						test.getId(), batchIndex, zeroProgress, maxZeroProgress);
				if (zeroProgress >= maxZeroProgress) {
					throw new IllegalStateException("Test generation failed for test " + test.getId() + ": "
							+ zeroProgress + " consecutive batches returned 0 valid questions. Generated "
							+ all.size() + "/" + totalQuestions
							+ " questions. Check validation rules, LLM prompt, or context quality.");
				}
			} else {
				zeroProgress = 0;
			}

			all.addAll(batch);
			log.info("    Test generation {} - Batch No. {} - Progress: {}/{} questions generated", test.getId(),
					batchIndex, all.size(), totalQuestions);
		}
		return all;
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
			test.setMinMultipleCorrectQuestionsCount(batchGenerator
					.computeMinMultipleCorrectQuestionsCount(test.getQuestionCount(), test.getAnswerCardinality()));
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

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private Long currentUserId() {
		var userId = executionContext.userId();
		return userId.isPresent() ? userId.getAsLong() : null;
	}

	@Override
	public Optional<Test> getById(Long testId) {
		return testRepository.findByTenantIdAndId(executionContext.tenantId(), testId);
	}

	@Override
	public List<Test> getAll() {
		return testRepository.findByTenantId(executionContext.tenantId());
	}

	@Override
	public List<Test> getAllByStatus(TestStatus status) {
		return testRepository.findByTenantIdAndStatus(executionContext.tenantId(), status);
	}

	@Override
	public Optional<List<TestQuestion>> getQuestionsByTestId(Long testId) {
		Long tenantId = executionContext.tenantId();
		if (testRepository.findByTenantIdAndId(tenantId, testId).isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(testQuestionRepository.findByTenantIdAndTestId(tenantId, testId));
	}

	@Override
	public void deleteById(Long testId) {
		testRepository.deleteByTenantIdAndId(executionContext.tenantId(), testId);
	}
}
