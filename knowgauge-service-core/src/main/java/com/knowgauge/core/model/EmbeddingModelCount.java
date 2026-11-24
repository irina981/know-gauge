package com.knowgauge.core.model;

/** How many chunk embeddings (of how many documents, all tenants) were made with one embedding model. */
public record EmbeddingModelCount(String model, long embeddings, long documents) {
}
