package com.knowgauge.core.port.testgeneration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

@Service
public class DummyLlmTestGenerationService implements LlmTestGenerationService{
	public List<TestQuestion> generate(String prompt, Test test) {
		return new ArrayList<TestQuestion>();	
	}

}
