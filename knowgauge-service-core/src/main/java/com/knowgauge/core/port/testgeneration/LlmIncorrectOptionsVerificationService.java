package com.knowgauge.core.port.testgeneration;

import java.util.List;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

/**
 * Port for LLM-based verification and replacement of unsafe incorrect options
 * in multiple-correct test questions.
 */
public interface LlmIncorrectOptionsVerificationService {

	/**
	 * Verifies and replaces unsafe incorrect options (distractors) in test questions.
	 * Only processes questions when the test uses MULTIPLE_CORRECT answer cardinality.
	 *
	 * @param questions the test questions to verify
	 * @param test      the test containing configuration
	 * @return number of options that were replaced
	 */
	int verifyAndReplaceUnsafeOptions(List<TestQuestion> questions, Test test);

}
