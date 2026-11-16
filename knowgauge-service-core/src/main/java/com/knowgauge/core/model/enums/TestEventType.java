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
	TEST_PUBLISHED,
	TEST_UNPUBLISHED,
	QUESTION_GENERATED,
	OPTION_FLAGGED,
	FIX_ACCEPTED,
	QUESTION_REGENERATED,
	REGENERATION_FAILED,
	QUESTION_MANUALLY_REPLACED,
	FLAGS_DISMISSED,
	/** The shared link of a published test was created (ADR009). */
	SHARED_LINK_CREATED,
	/** The shared link was replaced; the old one stopped working (ADR009). */
	SHARED_LINK_REGENERATED
}
