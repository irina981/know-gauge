package com.knowgauge.core.context;

/**
 * Whether the current request runs for an {@code APPLICATION_ADMIN} (ADR010). False outside requests (jobs,
 * startup seeding) and when sign-in is off.
 */
public interface ApplicationAdminProvider {
	boolean isApplicationAdmin();
}
