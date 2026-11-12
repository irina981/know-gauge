package com.knowgauge.core.service.testgeneration.validation;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

@Component
public class PostLlmFinalValidator {

	private final TestQuestionValidator validator;

	public PostLlmFinalValidator(TestQuestionValidator validator) {
		this.validator = validator;
	}

	public List<TestQuestion> validateAndNormalize(List<TestQuestion> questions, Test test,
			Collection<Long> availableChunkIds, int startIndex) {
		return validator.validateAndNormalize(questions, test, availableChunkIds, startIndex,
				TestQuestionValidator.postLlmStrategies());
	}
}
