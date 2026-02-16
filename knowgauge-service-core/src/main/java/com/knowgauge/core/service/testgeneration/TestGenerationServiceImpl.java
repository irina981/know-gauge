package com.knowgauge.core.service.testgeneration;

import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.core.port.testgeneration.LlmTestGenerationService;
import com.knowgauge.core.port.vectorstore.VectorStore;
import com.knowgauge.core.service.content.DocumentService;
import com.knowgauge.core.service.content.TopicService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TestGenerationServiceImpl implements TestGenerationService {

	private final TopicService topicService;
	private final DocumentService documentService;
	private final VectorStore vectorStore;

	private final TestPromptBuilder promptBuilder;
	private final LlmTestGenerationService llmTestGenerationService;
	private final TestGenerationValidator validator;
	private final TestGenerationTransactionalServiceImpl tx;
	private final DocumentChunkRepository documentChunkRepository;
	private final ExecutionContext executionContext;

	public TestGenerationServiceImpl(TopicService topicService, DocumentService documentService,
			VectorStore vectorStore, TestPromptBuilder promptBuilder, LlmTestGenerationService llmTestGenerationService,
			TestGenerationValidator validator, TestGenerationTransactionalServiceImpl tx,
			DocumentChunkRepository documentChunkRepository, ExecutionContext executionContext) {
		this.topicService = topicService;
		this.documentService = documentService;
		this.vectorStore = vectorStore;
		this.promptBuilder = promptBuilder;
		this.llmTestGenerationService = llmTestGenerationService;
		this.validator = validator;
		this.tx = tx;
		this.documentChunkRepository = documentChunkRepository;
		this.executionContext = executionContext;
	}

	@Override
	public Test generate(Test testDraft) {
		Long tenantId = executionContext.tenantId();
		testDraft.setTenantId(tenantId);

		validate(tenantId, testDraft);

		List<Long> topicIds = testDraft.getTopicIds();
		List<Long> documentIds = testDraft.getDocumentIds();

		log.info("*** Test generation started for topicIds={}, documentIds={}. questionCount={}", topicIds, documentIds,
				testDraft.getQuestionCount());

		// 1) Persist test and mark it GENERATING
		Test test = tx.persistTest(tenantId, testDraft);

		try {

			// 2) Select embeddings based on topis IDs, document IDs, chunk limit, coverage mode and avoiding repeats
			List<ChunkEmbedding> embeddings = vectorStore.findTop(tenantId, topicIds, documentIds, recommendedChunkLimit(test),
					test.getCoverageMode(), test.isAvoidRepeats());
			log.info("   Retrieved {} embeddings from vector store", embeddings.size());

	
			List<Long> chunkIds = embeddings.stream().map(ChunkEmbedding::getChunkId).toList();
			List<DocumentChunk> chunks = documentChunkRepository.findByTenantIdAndIdIn(tenantId, chunkIds);
			test.setUsedChunks(chunks);

			// 3) Build prompt
			String prompt = promptBuilder.buildPrompt(test, chunks);

			// 4) Generate questions
			List<TestQuestion> generatedTestQuestions = llmTestGenerationService.generate(prompt, test);
			log.info("   Generated {} questions", generatedTestQuestions.size());

			// 5) Validate questions
			List<TestQuestion> validatedTestQuestions = validator.validateAndNormalize(generatedTestQuestions, test, embeddings);

			// 6) Persist questions
			tx.persistTestQuestions(tenantId, test.getId(), validatedTestQuestions, embeddings);

			// 7) Mark test GENERATED
			Test ready = tx.markTestGenerated(tenantId, test.getId());

			log.info("*** Test generation completed for testId={}", ready.getId());

			return ready;

		} catch (Exception ex) {

			// Mark test FAILED
			tx.markTestFailed(tenantId, test.getId(), ex.getMessage());

			throw new RuntimeException("Test generation failed for test " + test.getId(), ex);
		}
	}

	private void validate(Long tenantId, Test test) {

		if (test == null)
			throw new IllegalArgumentException("Test must not be null");

		List<Long> topicIds = test.getTopicIds();
		List<Long> documentIds = test.getDocumentIds();

		if (topicIds.isEmpty() && documentIds.isEmpty()) {
			throw new IllegalArgumentException("Test must specify at least one topicId or documentId.");
		}

		topicIds.stream().forEach(topicId -> {
			Topic topic = topicService.get(topicId).orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));
			
			if (tenantId.equals(topic.getTenantId())) {
				 new IllegalArgumentException("Topic " + topicId + " does not belong to a tenant: " + tenantId);
			}

		});

		documentIds.stream().forEach(documentId -> {
			Document document = documentService.get(documentId).orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
			
			if (tenantId.equals(document.getTenantId())) {
				 new IllegalArgumentException("Document " + document + " does not belong to a tenant: " + tenantId);
			}

		});

		if (test.getQuestionCount() == null || test.getQuestionCount() <= 0)
			throw new IllegalArgumentException("Test.questionCount must be > 0");
	}

	private int recommendedChunkLimit(Test test) {
		return Math.max(20, test.getQuestionCount() * 4);
	}
}
