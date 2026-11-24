package com.knowgauge.core.context;

/**
 * Whether the current request is an {@code ADMIN} looking at another user's library, a tenant (ADR013). That view is
 * read-only: content of the viewed tenant is reported as not writable, and the API refuses changes.
 */
public interface TenantViewProvider {
	boolean isViewingOtherTenant();
}
