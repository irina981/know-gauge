package com.knowgauge.core.port.testgeneration;

import java.util.List;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

public interface LlmTestGenerationService {
	List<TestQuestion> generate(String prompt, Test test);

}
