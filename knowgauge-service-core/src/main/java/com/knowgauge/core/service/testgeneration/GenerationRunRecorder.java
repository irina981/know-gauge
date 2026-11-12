package com.knowgauge.core.service.testgeneration;

import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.GenerationRunPurpose;
import com.knowgauge.core.model.enums.GenerationRunStatus;
import com.knowgauge.core.port.repository.GenerationRunRepository;
import com.knowgauge.core.port.testgeneration.LlmCallMetadata;

import lombok.extern.slf4j.Slf4j;

/**
 * Writes one {@link GenerationRun} per LLM call. Each save commits on its own,
 * so the record survives a later failure of the generation flow.
 */
@Slf4j
@Component
public class GenerationRunRecorder {

	private final GenerationRunRepository repository;

	public GenerationRunRecorder(GenerationRunRepository repository) {
		this.repository = repository;
	}

	public record RunContext(Test test, Long questionId, GenerationRunPurpose purpose, Integer batchIndex,
			String promptTemplateId, List<Long> chunkIds) {
	}

	public GenerationRun success(RunContext ctx, LlmCallMetadata call) {
		return save(ctx, call, GenerationRunStatus.SUCCESS, null);
	}

	public GenerationRun failure(RunContext ctx, LlmCallMetadata call, String errorMessage) {
		return save(ctx, call, GenerationRunStatus.FAILED, errorMessage);
	}

	private GenerationRun save(RunContext ctx, LlmCallMetadata call, GenerationRunStatus status, String error) {
		Test test = ctx.test();
		GenerationRun run = GenerationRun.builder().tenantId(test.getTenantId()).testId(test.getId())
				.questionId(ctx.questionId()).purpose(ctx.purpose()).batchIndex(ctx.batchIndex())
				.model(call != null && call.model() != null ? call.model() : test.getGenerationModel())
				.promptTemplateId(ctx.promptTemplateId()).retrievedChunkIds(ctx.chunkIds())
				.rawResponse(call != null ? call.rawResponse() : null)
				.inputTokens(call != null ? call.inputTokens() : null)
				.outputTokens(call != null ? call.outputTokens() : null)
				.finishReason(call != null ? call.finishReason() : null).status(status).errorMessage(error).build();
		GenerationRun saved = repository.save(run);
		log.debug("    Test {} - Recorded generation run {} ({}, {})", test.getId(), saved.getId(), ctx.purpose(), status);
		return saved;
	}
}
