package com.knowgauge.core.model;

import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.enums.GenerationRunPurpose;
import com.knowgauge.core.model.enums.GenerationRunStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One LLM call made for a test: initial generation batch, regeneration of a
 * single question, or distractor verification. Keeps the prompt as sent, the
 * request settings and the raw response, so the call can be reconstructed and
 * audited even after prompt templates or chunks change.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GenerationRun extends AuditableObject {

	private Long tenantId;

	private Long testId;

	/** Set for REGENERATION runs (and their verification runs). */
	private Long questionId;

	private GenerationRunPurpose purpose;

	private Integer batchIndex;

	private String model;

	private String promptTemplateId;

	private List<Long> retrievedChunkIds;

	/** Full prompt text sent to the model, including any adapter suffix. */
	private String prompt;

	/**
	 * Request settings that shaped the call: temperature, max output tokens,
	 * response format, JSON schema. Null when the call failed before the adapter
	 * reported them.
	 */
	private Map<String, Object> requestParams;

	private String rawResponse;

	private Integer inputTokens;

	private Integer outputTokens;

	private String finishReason;

	private GenerationRunStatus status;

	private String errorMessage;
}
