package com.knowgauge.core.port.ingestion;

public interface EmbeddingService {
	float[] embed(String text);

	String modelName();

}
