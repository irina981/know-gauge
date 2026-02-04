package com.knowgauge.core.exception;

public class DuplicateDocumentContentException extends BaseException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DuplicateDocumentContentException(Long topicId) {
		super("Document with identical content already exists under topicId=" + topicId);
	}
}
