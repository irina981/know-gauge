package com.knowgauge.core.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * What a tenant holds and what it used in one period (a calendar month, UTC). Counts only the tenant's own content,
 * never the shared library it reads.
 */
@Getter
@Builder(toBuilder = true)
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
	/** What the LLM tokens cost in US dollars, with the configured token prices; {@code unpricedModels} not included. */
	private final BigDecimal llmCostUsd;
	/** Models used in the period that have no token price, so their tokens are missing from {@code llmCostUsd}. */
	private final List<String> unpricedModels;
	/** LLM tokens (input + output) the tenant may use this month; {@code null} when there is no limit. */
	private final Long llmTokenLimit;
	/** Bytes of documents the tenant may keep; {@code null} when there is no limit. */
	private final Long storageLimitBytes;
}
