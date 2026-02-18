package com.knowgauge.core.service.testgeneration;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.core.port.repository.TestRepository;

@Service
public class TestGenerationTransactionalServiceImpl {

	private final TestRepository testRepository;
	private final TestQuestionRepository testQuestionRepository;

	public TestGenerationTransactionalServiceImpl(TestRepository testRepository,
			TestQuestionRepository testQuestionRepository) {
		this.testRepository = testRepository;
		this.testQuestionRepository = testQuestionRepository;
	}

	@Transactional
	public Test persistTest(Long tenantId, Test toSave) {

		// If your Test has tenantId (not shown in your snippet), keep this line;
		// otherwise remove it.
		// toSave.setTenantId(tenantId);

		toSave.setStatus(TestStatus.CREATED);
		toSave.setGenerationStartedAt(Instant.now());

		Map<String, Object> params = toSave.getGenerationParams();
		if (params == null)
			params = new HashMap<>();
		params.remove("error"); // Just in case, remove any error info from previous failed attempt.
		toSave.setGenerationParams(params);

		return testRepository.save(toSave);
	}

	@Transactional
	public void persistTestQuestions(Long tenantId, Long testId, List<TestQuestion> questions,
			List<ChunkEmbedding> embeddings) {

		// 1) Determine default source chunk ids for this test run (if LLM didn't
		// provide per-question citations)
		// Option A: put ALL used chunkIds on each question (simple + consistent)
		// Later you can evolve to per-question sourceChunkIdsJson if your generator
		// produces it.
		List<Long> defaultSourceChunkIds = embeddings.stream().map(ChunkEmbedding::getChunkId).distinct()
				.collect(Collectors.toList());

		// 2) Prepare questions for persistence
		for (TestQuestion q : questions) {
			q.setTenantId(tenantId);
			q.setTestId(testId);

			// If generator already filled q.sourceChunkIdsJson, keep it.
			// Otherwise, set default (all used chunks).
			if (q.getSourceChunkIdsJson() == null || q.getSourceChunkIdsJson().isEmpty()) {
				q.setSourceChunkIdsJson(defaultSourceChunkIds);
			}
		}

		// 4) Persist
		testQuestionRepository.saveAll(questions);
	}

	@Transactional
	public Test markTestGenerated(Long tenantId, Long testId) {
	    int updated = testRepository.markGenerated(tenantId, testId, Instant.now());
	    if (updated == 0) throw new IllegalArgumentException("Test not found: " + testId);
	    return testRepository.findByTenantIdAndId(tenantId, testId).orElseThrow();
	}

	@Transactional
	public void markTestFailed(Long tenantId, Long testId, String errorMessage) {
	    int updated = testRepository.markFailed(tenantId, testId, errorMessage, Instant.now());
	    if (updated == 0) throw new IllegalArgumentException("Test not found: " + testId);
	}
	
	@Transactional
	public void setUsedChunks(Long tenantId, Long testId, List<Long> chunkIds) {
	    testRepository.setUsedChunks(tenantId, testId, chunkIds);
	}
}
