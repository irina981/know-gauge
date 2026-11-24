package com.knowgauge.core.service.settings;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

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
public class CoreSystemSettings implements SystemSettingsContributor {

	static final String MONTHLY_TOKEN_LIMIT = "kg.settings.tenant.monthly-token-limit";
	static final String STORAGE_LIMIT_MB = "kg.settings.tenant.storage-limit-mb";

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
	public List<SystemSettingDefinition> definitions() {
		return List.of(
				new SystemSettingDefinition(MONTHLY_TOKEN_LIMIT, "Workspace limits", "AI tokens per workspace and month",
						"Input and output tokens of question writing and checking, per calendar month (UTC). At the limit"
								+ " new tests and regenerations are refused until next month. Empty: no limit.",
						SystemSettingDefinition.Type.INTEGER, List.of(), 1L, null, true,
						() -> text(limits.getMonthlyTokenLimit()),
						value -> limits.setMonthlyTokenLimit(value == null ? null : Long.valueOf(value))),
				new SystemSettingDefinition(STORAGE_LIMIT_MB, "Workspace limits", "Storage per workspace (MB)",
						"Total size of the PDFs a workspace may keep, in megabytes. An upload that would go over it is"
								+ " refused; deleting documents frees space. The shared library has no limit. Empty: no limit.",
						SystemSettingDefinition.Type.INTEGER, List.of(), 1L, null, true,
						() -> text(limits.getStorageLimitMb()),
						value -> limits.setStorageLimitMb(value == null ? null : Long.valueOf(value))),
				new SystemSettingDefinition("kg.testgen.defaults.generation-model", "AI models",
						"Question writing model",
						"The OpenAI model that writes new questions. The list shows the OpenAI chat models of the price"
								+ " catalog. Give the model a token price too, so its cost shows.",
						SystemSettingDefinition.Type.TEXT, List.of(), null, null, false, testgen::getGenerationModel,
						testgen::setGenerationModel, () -> priceCatalog.chatModels("openai")),
				new SystemSettingDefinition("kg.testgen.defaults.question-generation-batch-size", "Test generation",
						"Questions per LLM call",
						"How many questions one call writes. Smaller batches are slower but fail less often.",
						SystemSettingDefinition.Type.INTEGER, List.of(), 1L, 20L, false,
						() -> text(testgen.getQuestionGenerationBatchSize()),
						value -> testgen.setQuestionGenerationBatchSize(Integer.valueOf(value))),
				new SystemSettingDefinition("kg.testgen.defaults.question-count", "Test generation",
						"Default number of questions", "Used when a request does not say how many questions to write.",
						SystemSettingDefinition.Type.INTEGER, List.of(), 1L, 100L, false,
						() -> text(testgen.getQuestionCount()), value -> testgen.setQuestionCount(Integer.valueOf(value))),
				new SystemSettingDefinition("kg.testgen.defaults.difficulty", "Test generation", "Default difficulty",
						"Used when a request does not choose a difficulty.", SystemSettingDefinition.Type.CHOICE,
						Arrays.stream(TestDifficulty.values()).map(Enum::name).toList(), null, null, false,
						() -> text(testgen.getDifficulty()),
						value -> testgen.setDifficulty(TestDifficulty.valueOf(value))),
				new SystemSettingDefinition("kg.invitations.max-per-request", "Invitations",
						"Invitations per request", "The most email addresses one invitation request may contain.",
						SystemSettingDefinition.Type.INTEGER, List.of(), 1L, 1000L, false,
						() -> text(invitations.getMaxPerRequest()),
						value -> invitations.setMaxPerRequest(Integer.parseInt(value))),
				new SystemSettingDefinition("kg.ingestion.job.batch-size", "Ingestion", "Documents per scheduled run",
						"How many uploaded documents the scheduled ingestion job ingests per run (oldest first).",
						SystemSettingDefinition.Type.INTEGER, List.of(), 1L, 500L, false,
						() -> text(ingestion.getBatchSize()), value -> ingestion.setBatchSize(Integer.parseInt(value))));
	}

	private static String text(Object value) {
		return value == null ? null : value.toString();
	}
}
