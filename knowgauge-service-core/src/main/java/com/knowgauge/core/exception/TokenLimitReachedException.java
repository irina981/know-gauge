package com.knowgauge.core.exception;

/** The workspace used its monthly AI token allowance (system setting): no new generation until next month. */
public class TokenLimitReachedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public TokenLimitReachedException(String message) {
		super(message);
	}
}
