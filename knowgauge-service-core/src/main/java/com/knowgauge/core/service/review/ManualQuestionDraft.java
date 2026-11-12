package com.knowgauge.core.service.review;

import java.util.Map;

import com.knowgauge.core.model.enums.OptionLetter;

/**
 * A question written by a reviewer for the MANUAL_REPLACE action.
 *
 * @param expectedCurrentVersionId version the reviewer was looking at
 * @param questionText             question text
 * @param options                  options A–D with their answer key
 * @param explanation              optional explanation
 * @param comment                  optional review comment for the history
 */
public record ManualQuestionDraft(Long expectedCurrentVersionId, String questionText,
		Map<OptionLetter, ManualOption> options, String explanation, String comment) {

	public record ManualOption(String text, boolean correct) {
	}
}
