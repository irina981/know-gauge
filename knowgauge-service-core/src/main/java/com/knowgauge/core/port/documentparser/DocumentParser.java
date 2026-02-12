package com.knowgauge.core.port.documentparser;

import java.io.InputStream;
import java.util.List;

public interface DocumentParser {
	  List<String> extractPages(InputStream in) throws Exception;
	  
	  String contentType();
}
