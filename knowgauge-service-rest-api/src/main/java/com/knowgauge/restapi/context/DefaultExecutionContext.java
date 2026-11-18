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

	private record Scope(long tenantId, OptionalLong userId) {
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
		return scope != null ? scope.tenantId() : tenantProvider.tenantId();
	}

	@Override
	public OptionalLong userId() {
		Scope scope = SCOPES.get().peek();
		return scope != null ? scope.userId() : userProvider.userId();
	}

	@Override
	public <T> T callAs(long tenantId, OptionalLong userId, Supplier<T> task) {
		Deque<Scope> scopes = SCOPES.get();
		scopes.push(new Scope(tenantId, userId == null ? OptionalLong.empty() : userId));
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
