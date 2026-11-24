package com.knowgauge.restapi.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalLong;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.knowgauge.core.context.CurrentTenantProvider;
import com.knowgauge.core.context.CurrentUserProvider;
import com.knowgauge.core.context.ExecutionContext;

/**
 * The tenant and user come from the providers of the security mode (fixed, or from the signed-in user's token),
 * unless {@link #callAs} set them for the current thread (jobs, startup seeding).
 */
@Component
public class DefaultExecutionContext implements ExecutionContext {

	/** {@code tenantId} is {@code null} for system work in no tenant ({@link #callAsSystemWithoutTenant}). */
	private record Scope(Long tenantId, OptionalLong userId) {
	}

	private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

	private final CurrentTenantProvider tenantProvider;
	private final CurrentUserProvider userProvider;

	public DefaultExecutionContext(CurrentTenantProvider tenantProvider, CurrentUserProvider userProvider) {
		this.tenantProvider = tenantProvider;
		this.userProvider = userProvider;
	}

	@Override
	public long tenantId() {
		Scope scope = SCOPES.get().peek();
		if (scope == null) {
			return tenantProvider.tenantId();
		}
		if (scope.tenantId() == null) {
			throw new IllegalStateException("No tenant: this work runs as the system user in no tenant");
		}
		return scope.tenantId();
	}

	@Override
	public OptionalLong userId() {
		Scope scope = SCOPES.get().peek();
		return scope != null ? scope.userId() : userProvider.userId();
	}

	@Override
	public <T> T callAs(long tenantId, OptionalLong userId, Supplier<T> task) {
		return callIn(new Scope(tenantId, userId == null ? OptionalLong.empty() : userId), task);
	}

	@Override
	public <T> T callAsSystemWithoutTenant(Supplier<T> task) {
		return callIn(new Scope(null, OptionalLong.of(SYSTEM_USER_ID)), task);
	}

	private static <T> T callIn(Scope scope, Supplier<T> task) {
		Deque<Scope> scopes = SCOPES.get();
		scopes.push(scope);
		try {
			return task.get();
		} finally {
			scopes.pop();
			if (scopes.isEmpty()) {
				SCOPES.remove();
			}
		}
	}
}
