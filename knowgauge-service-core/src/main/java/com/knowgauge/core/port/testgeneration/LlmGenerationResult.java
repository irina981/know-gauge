package com.knowgauge.core.port.testgeneration;

import java.util.List;

import com.knowgauge.core.model.TestQuestion;

/**
 * Draft questions parsed from one generation call, plus the call metadata.
 */
public record LlmGenerationResult(List<TestQuestion> questions, LlmCallMetadata call) {
}
