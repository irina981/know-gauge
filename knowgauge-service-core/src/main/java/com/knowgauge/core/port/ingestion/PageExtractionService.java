package com.knowgauge.core.port.ingestion;

import java.io.InputStream;
import java.util.List;

public interface PageExtractionService {
	  List<String> extractPages(InputStream in) throws Exception;
	  
	  String getSupportedContentType();
}
