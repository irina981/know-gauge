package com.knowgauge.core.exception;

public class DuplicateDocumentNameException extends BaseException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DuplicateDocumentNameException(Long topicId, String name) {
        super("Document with name '" + name + "' already exists under topicId=" + topicId);
    }
}