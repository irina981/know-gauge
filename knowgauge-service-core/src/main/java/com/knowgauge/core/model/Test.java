package com.knowgauge.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.knowgauge.core.model.enums.Language;
import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.model.enums.TestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Test extends AuditableObject {

	// ============================================================
	// Ownership
	// ============================================================

	private Long tenantId;

	// ============================================================
	// Coverage scope
	// ============================================================

	@Builder.Default
	private List<Long> topicIds = new ArrayList<>();

	@Builder.Default
	private List<Long> documentIds = new ArrayList<>();

	/**
	 * Chunks actually used for generation. Set after vector retrieval and persisted
	 * separately.
	 */
	@Builder.Default
	private List<DocumentChunk> usedChunks = new ArrayList<>();

	// ============================================================
	// Test configuration
	// ============================================================

	private TestDifficulty difficulty;

	private Boolean avoidRepeats;

	private TestCoverageMode coverageMode;

	private Integer questionCount;

	private AnswerCardinality answerCardinality;

	private Language language;

	// ============================================================
	// Generation configuration
	// ============================================================

	private TestStatus status;

	/**
	 * LLM model used for generation (ex: gpt-4o, gpt-4.1, etc.)
	 */
	private String generationModel;

	/**
	 * Prompt template identifier used for generation.
	 */
	private String promptTemplateId;

	/**
	 * Optional structured parameters used during generation. Stored as JSON in
	 * persistence layer.
	 */
	private Map<String, Object> generationParams;

	// ============================================================
	// Generation lifecycle tracking
	// ============================================================

	/**
	 * When generation started.
	 */
	private Instant generationStartedAt;

	/**
	 * When generation successfully finished.
	 */
	private Instant generationFinishedAt;

	/**
	 * When generation failed.
	 */
	private Instant generationFailedAt;

	/**
	 * Error message if generation failed.
	 */
	private String generationErrorMessage;

}
