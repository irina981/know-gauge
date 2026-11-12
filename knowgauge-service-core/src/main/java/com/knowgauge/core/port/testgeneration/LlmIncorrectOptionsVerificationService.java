package com.knowgauge.core.port.testgeneration;

import java.util.List;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.TestQuestion;

/**
 * Port for LLM-based verification of incorrect options (distractors).
 *
 * <p>
 * The verifier only <em>flags</em> distractors it judges to be true, attaching a
 * clearly false suggested replacement and a reason. It never changes the answer
 * key (see ADR001). Verification is best effort: failures are reported in the
 * result, never thrown.
 */
public interface LlmIncorrectOptionsVerificationService {

	/**
	 * @param prompt    rendered verification prompt. Question indexes in it are
	 *                  positions in {@code questions}
	 * @param test      test configuration (model overrides in generation params)
	 * @param questions draft questions, flagged in place
	 */
	LlmVerificationResult verify(String prompt, Test test, List<TestQuestion> questions);
}
