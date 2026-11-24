package com.knowgauge.core.service.settings;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.service.ingestion.IngestionJobProperties;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.testgeneration.TestGenerationDefaultsProperties;

/**
 * The settings of the core modules on the System settings page. Each is read on every use, so a change applies at
 * once. Infrastructure (datasources, schedules, upload sizes, API keys) stays in the properties files only.
 */
@Component
public class CoreSystemSettings implements SettingsContributor {

	/**
	 * The section of every limit on the System settings page, which shows system values only. Whether a workspace may
	 * have a value of its own is decided per key in setting_config_owners, for limits and configuration alike.
	 */
	private static final String SYSTEM_LIMITS = "System limits";

	private final TenantLimitProperties limits;
	private final TestGenerationDefaultsProperties testgen;
	private final InvitationProperties invitations;
	private final IngestionJobProperties ingestion;
	private final TokenPriceCatalog priceCatalog;

	public CoreSystemSettings(TenantLimitProperties limits, TestGenerationDefaultsProperties testgen,
			InvitationProperties invitations, IngestionJobProperties ingestion, TokenPriceCatalog priceCatalog) {
		this.priceCatalog = priceCatalog;
		this.limits = limits;
		this.testgen = testgen;
		this.invitations = invitations;
		this.ingestion = ingestion;
	}

	@Override
	public List<SettingDefinition> definitions() {
		return List.of(
				// The workspace limits (TenantLimits). Like every setting, their type and owners are in setting_configs /
				// setting_config_owners (ADR014); for now they also have a TENANT owner. 0: no limit.
				new SettingDefinition(TenantLimits.MONTHLY_TOKENS, SYSTEM_LIMITS, "AI tokens per workspace and month",
						"Input and output tokens of question writing and checking, per calendar month (UTC). At the limit"
								+ " new tests and regenerations are refused until next month. 0: no limit.",
						SettingDefinition.Type.INTEGER, List.of(), 0L, null, false,
						() -> text(limits.getMonthlyTokenLimit()),
						value -> limits.setMonthlyTokenLimit(value == null ? null : Long.valueOf(value))),
				new SettingDefinition(TenantLimits.STORAGE_MB, SYSTEM_LIMITS, "Storage per workspace (MB)",
						"Total size of the PDFs a workspace may keep, in megabytes. An upload that would go over it is"
								+ " refused; deleting documents frees space. The shared library has no limit. 0: no limit.",
						SettingDefinition.Type.INTEGER, List.of(), 0L, null, false,
						() -> text(limits.getStorageLimitMb()),
						value -> limits.setStorageLimitMb(value == null ? null : Long.valueOf(value))),
				new SettingDefinition(TenantLimits.CONCURRENT_GENERATIONS, SYSTEM_LIMITS,
						"Tests generating at once per user",
						"How many tests one user may generate at the same time. Another request is refused until one of"
								+ " them is ready. 0: no limit.",
						SettingDefinition.Type.INTEGER, List.of(), 0L, 20L, false,
						() -> text(limits.getConcurrentGenerationsPerUser()),
						value -> limits.setConcurrentGenerationsPerUser(value == null ? null : Integer.valueOf(value))),
				new SettingDefinition(TenantLimits.PARTICIPANTS_PER_TEST, SYSTEM_LIMITS, "Participants per test",
						"How many different people one test may have: everyone invited to it and everyone who started it"
								+ " from the shared link, each counted once. Once a test is full, more invitations and new"
								+ " people on the shared link are refused. 0: no limit.",
						SettingDefinition.Type.INTEGER, List.of(), 0L, null, false,
						() -> text(limits.getParticipantsPerTest()),
						value -> limits.setParticipantsPerTest(value == null ? null : Integer.valueOf(value))),
				new SettingDefinition(ConfigKeys.GENERATION_MODEL, "AI models",
						"Question writing model",
						"The OpenAI model that writes new questions. The list shows the OpenAI chat models of the price"
								+ " catalog. Give the model a token price too, so its cost shows.",
						SettingDefinition.Type.TEXT, List.of(), null, null, false, testgen::getGenerationModel,
						testgen::setGenerationModel, () -> priceCatalog.chatModels("openai")),
				new SettingDefinition(ConfigKeys.QUESTIONS_PER_CALL, "Test generation",
						"Questions per LLM call",
						"How many questions one call writes. Smaller batches are slower but fail less often.",
						SettingDefinition.Type.INTEGER, List.of(), 1L, 20L, false,
						() -> text(testgen.getQuestionGenerationBatchSize()),
						value -> testgen.setQuestionGenerationBatchSize(Integer.valueOf(value))),
				new SettingDefinition(ConfigKeys.QUESTION_COUNT, "Test generation",
						"Default number of questions", "Used when a request does not say how many questions to write.",
						SettingDefinition.Type.INTEGER, List.of(), 1L, 100L, false,
						() -> text(testgen.getQuestionCount()), value -> testgen.setQuestionCount(Integer.valueOf(value))),
				new SettingDefinition(ConfigKeys.DIFFICULTY, "Test generation", "Default difficulty",
						"Used when a request does not choose a difficulty.", SettingDefinition.Type.CHOICE,
						Arrays.stream(TestDifficulty.values()).map(Enum::name).toList(), null, null, false,
						() -> text(testgen.getDifficulty()),
						value -> testgen.setDifficulty(TestDifficulty.valueOf(value))),
				new SettingDefinition("kg.invitations.max-per-request", SYSTEM_LIMITS,
						"Max invitations sent at once",
						"The most email addresses one invitation can go to at once, for a test or a workspace. Invite"
								+ " more people by sending again.",
						SettingDefinition.Type.INTEGER, List.of(), 1L, 1000L, false,
						() -> text(invitations.getMaxPerRequest()),
						value -> invitations.setMaxPerRequest(Integer.parseInt(value))),
				new SettingDefinition("kg.ingestion.job.batch-size", "Ingestion", "Documents per scheduled run",
						"How many uploaded documents the scheduled ingestion job ingests per run (oldest first).",
						SettingDefinition.Type.INTEGER, List.of(), 1L, 500L, false,
						() -> text(ingestion.getBatchSize()), value -> ingestion.setBatchSize(Integer.parseInt(value))));
	}

	private static String text(Object value) {
		return value == null ? null : value.toString();
	}
}
