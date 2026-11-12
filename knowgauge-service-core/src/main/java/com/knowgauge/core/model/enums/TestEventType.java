package com.knowgauge.core.model.enums;

/**
 * Types of entries in the append-only history trail of a test.
 */
public enum TestEventType {
	TEST_CREATED,
	TEST_GENERATED,
	TEST_REQUIRES_REVIEW,
	TEST_REVIEWED,
	TEST_FAILED,
	QUESTION_GENERATED,
	OPTION_FLAGGED,
	FIX_ACCEPTED,
	QUESTION_REGENERATED,
	REGENERATION_FAILED,
	QUESTION_MANUALLY_REPLACED
}
