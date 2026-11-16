package com.knowgauge.core.exception;

/**
 * The selected documents yield too few chunks for the requested number of
 * questions ({@code kg.testgen.defaults.min-chunks-per-question}). Maps to HTTP
 * 422.
 */
public class InsufficientContextException extends BaseException {

	private static final long serialVersionUID = 1L;

	public InsufficientContextException(int availableChunks, int requiredChunks, int questionCount) {
		super("The selected documents have " + availableChunks + " chunks, but " + questionCount + " questions need at least "
				+ requiredChunks + ". Reduce the question count or add documents.");
	}

	public InsufficientContextException(String message) {
		super(message);
	}

	/** FOCUSED coverage found too few chunks close to the focus text. */
	/**
	 * @param maxQuestions the most questions the matching chunks allow
	 */
	public static InsufficientContextException forFocus(int matchingChunks, int requiredChunks, int questionCount,
			int maxQuestions) {
		if (matchingChunks == 0) {
			return new InsufficientContextException("None of the selected material is close to the focus text. "
					+ "Check that the selected topics or documents cover it, or rephrase the focus.");
		}
		return new InsufficientContextException("Only " + matchingChunks
				+ " passages of the selected material are close to the focus text, but " + questionCount
				+ " questions need at least " + requiredChunks + ". Ask for at most " + maxQuestions
				+ " questions, or make the focus broader.");
	}
}
