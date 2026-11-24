package com.knowgauge.contract.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What the user's workspace holds, and what it used in the current calendar month (UTC). The shared library is not counted.")
public record UsageSummaryDto(
		@Schema(description = "Start of the month, inclusive") Instant periodStart,
		@Schema(description = "Start of the next month, exclusive") Instant periodEnd,
		long topics,
		long documents,
		@Schema(description = "Documents ready for test generation") long ingestedDocuments,
		@Schema(description = "Total size of the uploaded PDFs in bytes") long storageBytes,
		@Schema(description = "Tests created this month") long tests,
		@Schema(description = "Questions the AI wrote this month: new tests and regenerations") long questions,
		@Schema(description = "Attempts participants submitted this month") long submittedAttempts,
		@Schema(description = "LLM input tokens of question writing and checking this month") long llmInputTokens,
		@Schema(description = "LLM output tokens of question writing and checking this month") long llmOutputTokens) {
}
