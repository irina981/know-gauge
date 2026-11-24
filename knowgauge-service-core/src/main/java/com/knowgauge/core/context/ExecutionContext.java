package com.knowgauge.core.context;

import java.util.OptionalLong;
import java.util.function.Supplier;

/**
 * Who the current work runs for. In a request it is the authenticated user and
 * their tenant. Work outside a request (scheduled jobs, startup seeding) has no
 * user and sets the tenant explicitly with {@link #runAs}.
 */
public interface ExecutionContext {

	/**
	 * Id of the reserved {@code system} user (row 0 in {@code users}, in no
	 * tenant): the actor of work no person started, such as scheduled jobs.
	 */
	long SYSTEM_USER_ID = 0L;

	/**
	 * @throws IllegalStateException when no tenant is known (an anonymous request
	 *                               or a job outside {@link #runAs})
	 */
	long tenantId();

	OptionalLong userId(); // may be absent (jobs, anonymous)

	/**
	 * Runs {@code task} on this thread with the given tenant and user as the
	 * context, then restores the previous one. Calls may nest.
	 */
	<T> T callAs(long tenantId, OptionalLong userId, Supplier<T> task);

	default void runAs(long tenantId, OptionalLong userId, Runnable task) {
		callAs(tenantId, userId, () -> {
			task.run();
			return null;
		});
	}

	/** Runs {@code task} for the tenant as the {@link #SYSTEM_USER_ID system} user (jobs, startup seeding). */
	default void runAsSystem(long tenantId, Runnable task) {
		runAs(tenantId, OptionalLong.of(SYSTEM_USER_ID), task);
	}

	/**
	 * Runs {@code task} as the {@link #SYSTEM_USER_ID system} user in no tenant: work the app does on its own
	 * account, such as creating a user and their tenant at the first sign-in. {@link #tenantId()} throws inside it.
	 */
	<T> T callAsSystemWithoutTenant(Supplier<T> task);
}
