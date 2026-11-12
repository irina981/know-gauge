package com.knowgauge.core.model.enums;

/**
 * Review status of a question slot, derived from its current version.
 */
public enum QuestionStatus {
	/** Current version has no flagged options. */
	OK,
	/** Current version has at least one option flagged by the verifier. */
	NEEDS_REVIEW
}
