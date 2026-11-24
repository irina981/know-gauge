package com.knowgauge.core.exception;

/**
 * A test has as many participants as its workspace allows (limit "Participants per test", ADR014): no more
 * invitations, and no new people from the shared link. Maps to HTTP 409.
 */
public class ParticipantLimitReachedException extends BaseException {

	private static final long serialVersionUID = 1L;

	public ParticipantLimitReachedException(String message) {
		super(message);
	}
}
