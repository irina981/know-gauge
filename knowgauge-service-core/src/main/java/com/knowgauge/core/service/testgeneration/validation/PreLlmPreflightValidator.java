package com.knowgauge.core.service.testgeneration.validation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

@Component
public class PreLlmPreflightValidator {

	private final TestQuestionValidator validator;

	public PreLlmPreflightValidator(TestQuestionValidator validator) {
		this.validator = validator;
	}

	public List<TestQuestion> validateAndNormalize(List<TestQuestion> questions, Test test,
			List<ChunkEmbedding> embeddings, int generatedCount) {
		return validator.validateAndNormalize(questions, test, embeddings, generatedCount,
				TestQuestionValidator.preLlmStrategies());
	}
}
