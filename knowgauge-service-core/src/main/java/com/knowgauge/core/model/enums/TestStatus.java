package com.knowgauge.core.model.enums;

/**
 * Lifecycle of a test. Generation moves a test from {@code CREATED} to
 * {@code GENERATED}, {@code REQUIRES_REVIEW} or {@code FAILED}; review moves it
 * between {@code GENERATED}/{@code REQUIRES_REVIEW}/{@code REVIEWED}. Publishing
 * (ADR005) freezes the content: a {@code PUBLISHED} test can no longer be
 * changed, and only published tests accept attempts.
 */
public enum TestStatus {
    CREATED,
    GENERATED,
    REQUIRES_REVIEW,
    REVIEWED,
    FAILED,
    PUBLISHED;

    /**
     * @return whether review actions may change the test's questions
     */
    public boolean isReviewable() {
        return this == GENERATED || this == REQUIRES_REVIEW || this == REVIEWED;
    }

    /**
     * @return whether the test may be published: generated, with no question
     *         waiting for review
     */
    public boolean isPublishable() {
        return this == GENERATED || this == REVIEWED;
    }

    /**
     * @return whether attempts may be started on the test
     */
    public boolean acceptsAttempts() {
        return this == PUBLISHED;
    }
}
