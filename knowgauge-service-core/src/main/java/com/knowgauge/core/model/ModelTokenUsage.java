package com.knowgauge.core.model;

/** Tokens of the LLM calls made with one model, as the provider named it. */
public record ModelTokenUsage(String model, long inputTokens, long outputTokens) {
}
