package com.knowgauge.core.service.settings;

/**
 * Keys of the configuration settings (type {@code CONFIG}) the code reads by name. Whether a workspace may have its
 * own value of one is decided by the database ({@code setting_config_owners}, ADR014); test generation uses a
 * workspace's own value when it has one ({@link TenantSettingsService#ownValues}), else the system value.
 */
public final class ConfigKeys {

	public static final String GENERATION_MODEL = "kg.testgen.defaults.generation-model";
	/** Defined by the testgeneration module ({@code VerificationSystemSettings}). */
	public static final String VERIFICATION_MODEL = "kg.testgen.verification.chat-model.openai.model";
	public static final String QUESTIONS_PER_CALL = "kg.testgen.defaults.question-generation-batch-size";
	public static final String QUESTION_COUNT = "kg.testgen.defaults.question-count";
	public static final String DIFFICULTY = "kg.testgen.defaults.difficulty";

	private ConfigKeys() {
	}
}
