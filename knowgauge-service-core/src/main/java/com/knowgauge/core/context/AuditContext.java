package com.knowgauge.core.context;

public final class AuditContext {

	private static final ThreadLocal<String> ACTOR = new ThreadLocal<>();

	private AuditContext() {
	}

	public static String currentActor() {
		String actor = ACTOR.get();
		return actor != null ? actor : "system";
	}

	public static void setActor(String actor) {
		ACTOR.set(actor);
	}

	public static void clear() {
		ACTOR.remove();
	}
}
