package com.knowgauge.core.context;

/**
 * Whether the current request runs for an application admin: a user with the {@code ADMIN} role (ADR012). False
 * outside requests (jobs, startup seeding) and when sign-in is off.
 */
public interface ApplicationAdminProvider {
	boolean isApplicationAdmin();
}
