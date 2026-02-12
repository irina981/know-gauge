package com.knowgauge.core.port.ingestion;

import java.util.List;

public interface EmbeddingService {
	float[] embed(String text);
	
	List<float[]> embed(List<String> texts);

	String modelName();

}
