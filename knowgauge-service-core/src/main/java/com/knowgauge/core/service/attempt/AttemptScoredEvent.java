package com.knowgauge.core.service.attempt;

import com.knowgauge.core.model.Attempt;

/**
 * Published when an attempt is scored, inside the scoring transaction.
 * Listeners that call external systems use {@code AFTER_COMMIT}.
 */
public record AttemptScoredEvent(Attempt attempt, String testName) {
}
