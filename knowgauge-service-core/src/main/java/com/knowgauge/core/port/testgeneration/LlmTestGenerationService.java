package com.knowgauge.core.port.testgeneration;

import com.knowgauge.core.model.Test;

public interface LlmTestGenerationService {

	/**
	 * Generates draft questions (options and answer key, no flags).
	 *
	 * @throws com.knowgauge.core.exception.LlmResponseParsingException when the
	 *                                                                   response
	 *                                                                   is cut off
	 *                                                                   or cannot
	 *                                                                   be parsed
	 */
	LlmGenerationResult generate(String prompt, Test test);
}
