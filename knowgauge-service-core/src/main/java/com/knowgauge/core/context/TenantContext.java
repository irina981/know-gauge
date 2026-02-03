package com.knowgauge.core.context;

public class TenantContext {
	private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

	private TenantContext() {
	}

	public static String currentTenant() {
		String tenant = TENANT.get();
		return tenant != null ? tenant : "system";
	}

	public static void setTenant(String tenant) {
		TENANT.set(tenant);
	}

	public static void clear() {
		TENANT.remove();
	}
}
