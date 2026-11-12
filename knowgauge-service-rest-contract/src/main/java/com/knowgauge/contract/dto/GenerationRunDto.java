package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One LLM call made for a test")
public record GenerationRunDto(
		@Schema(description = "Run identifier") Long id,
		@Schema(description = "Test identifier") Long testId,
		@Schema(description = "Question identifier (regeneration only)") Long questionId,
		@Schema(description = "TEST_GENERATION, REGENERATION or VERIFICATION") String purpose,
		@Schema(description = "Batch number within the generation") Integer batchIndex,
		@Schema(description = "Model used", example = "gpt-4.1") String model,
		@Schema(description = "Prompt template id", example = "mcq-default") String promptTemplateId,
		@Schema(description = "Context chunk ids") List<Long> retrievedChunkIds,
		@Schema(description = "Full prompt text sent to the model") String prompt,
		@Schema(description = "Request settings: temperature, max output tokens, response format, JSON schema") Map<String, Object> requestParams,
		@Schema(description = "Raw model output") String rawResponse,
		@Schema(description = "Prompt tokens") Integer inputTokens,
		@Schema(description = "Completion tokens") Integer outputTokens,
		@Schema(description = "Provider finish reason", example = "STOP") String finishReason,
		@Schema(description = "SUCCESS or FAILED") String status,
		@Schema(description = "Error when FAILED") String errorMessage,
		@Schema(description = "When the call was recorded (UTC)") Instant createdAt) {
}
