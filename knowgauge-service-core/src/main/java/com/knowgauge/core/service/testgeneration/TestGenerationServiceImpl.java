package com.knowgauge.core.service.testgeneration;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.core.port.vectorstore.VectorStore;
import com.knowgauge.core.service.testgeneration.validation.TestDraftValidator;
import com.knowgauge.core.service.testgeneration.validation.TestQuestionValidator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TestGenerationServiceImpl implements TestGenerationService {

	private final VectorStore vectorStore;

	private final TestPromptBuilder promptBuilder;
	private final LlmTestGenerationService llmTestGenerationService;
	private final TestQuestionValidator testQuestionValidator;
	private final TestDraftValidator testDraftValidator;
	private final TestGenerationTransactionalServiceImpl tx;
	private final TestRepository testRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final ExecutionContext executionContext;
	private final TestGenerationDefaultsProperties defaults;

	public TestGenerationServiceImpl(VectorStore vectorStore, TestPromptBuilder promptBuilder,
			LlmTestGenerationService llmTestGenerationService, TestQuestionValidator testQuestionValidator,
			TestDraftValidator testDraftValidator, TestGenerationTransactionalServiceImpl tx,
			TestRepository testRepository, DocumentChunkRepository documentChunkRepository,
			ExecutionContext executionContext, TestGenerationDefaultsProperties defaults) {
		this.vectorStore = vectorStore;
		this.promptBuilder = promptBuilder;
		this.llmTestGenerationService = llmTestGenerationService;
		this.testQuestionValidator = testQuestionValidator;
		this.testDraftValidator = testDraftValidator;
		this.tx = tx;
		this.testRepository = testRepository;
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
		log.info("    Test generation {} - Test persisted", test.getId());

		try {

			// 2) Select embeddings based on topis IDs, document IDs, chunk limit, coverage
			// mode and avoiding repeats
			List<ChunkEmbedding> embeddings = vectorStore.findTop(tenantId, documentIds, recommendedChunkLimit(test),
					test.getCoverageMode(), Boolean.TRUE.equals(test.getAvoidRepeats()));
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

			// 3) Build prompt
			String prompt = promptBuilder.buildPrompt(test, chunks);
			log.debug("    Test generation {} - Built prompt: {}", test.getId(), prompt);

			// 4) Generate questions
			List<TestQuestion> generatedTestQuestions = llmTestGenerationService.generate(prompt, test);
			log.info("    Test generation {} - Generated {} questions.", test.getId(), generatedTestQuestions.size());

			// 5) Validate questions
			List<TestQuestion> validatedTestQuestions = testQuestionValidator
					.validateAndNormalize(generatedTestQuestions, test, embeddings);

			// 6) Persist questions
			tx.persistTestQuestions(tenantId, test.getId(), validatedTestQuestions, embeddings);

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

	private int recommendedChunkLimit(Test test) {
		return Math.max(20, test.getQuestionCount() * 4);
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
	public void deleteById(Long testId) {
		Long tenantId = executionContext.tenantId();
		testRepository.deleteByTenantIdAndId(tenantId, testId);
	}
}
