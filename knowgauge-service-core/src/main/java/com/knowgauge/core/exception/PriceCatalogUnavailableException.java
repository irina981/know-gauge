package com.knowgauge.core.exception;

/** The token price catalog could not be downloaded or read. */
public class PriceCatalogUnavailableException extends BaseException {

	private static final long serialVersionUID = 1L;

	public PriceCatalogUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
