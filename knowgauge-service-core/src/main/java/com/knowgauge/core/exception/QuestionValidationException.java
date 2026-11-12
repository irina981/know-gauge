package com.knowgauge.core.exception;

import java.util.List;

/**
 * A manually written question failed the validation rules. Maps to HTTP 400 and
 * reports every reason.
 */
public class QuestionValidationException extends BaseException {

	private static final long serialVersionUID = 1L;

	private final List<String> reasons;

	public QuestionValidationException(List<String> reasons) {
		super("Question is invalid: " + String.join("; ", reasons));
		this.reasons = List.copyOf(reasons);
	}

	public List<String> getReasons() {
		return reasons;
	}
}
