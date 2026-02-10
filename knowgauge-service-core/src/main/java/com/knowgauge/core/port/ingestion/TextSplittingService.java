package com.knowgauge.core.port.ingestion;

import java.util.List;

public interface TextSplittingService {
	List<String> split(String text);
}
