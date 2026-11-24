package com.knowgauge.core.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

/**
 * What a tenant holds and what it used in one period (a calendar month, UTC). Counts only the tenant's own content,
 * never the shared library it reads.
 */
@Getter
@Builder
public class UsageSummary {

	/** Start of the period, inclusive. */
	private final Instant periodStart;
	/** End of the period, exclusive. */
	private final Instant periodEnd;

	private final long topics;
	private final long documents;
	private final long ingestedDocuments;
	/** Total size of the uploaded PDFs. */
	private final long storageBytes;

	/** Tests created in the period, whatever their status. */
	private final long tests;
	/** Questions written in the period: new tests and regenerated questions. */
	private final long questions;
	/** Attempts participants submitted in the period. */
	private final long submittedAttempts;

	/** LLM tokens of the generation and verification calls in the period (ingestion embeddings not yet counted). */
	private final long llmInputTokens;
	private final long llmOutputTokens;
}
