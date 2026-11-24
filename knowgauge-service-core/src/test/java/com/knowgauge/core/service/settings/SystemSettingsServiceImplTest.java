package com.knowgauge.core.service.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Setting;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.model.SettingConfig;
import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.core.port.repository.SettingConfigRepository;
import com.knowgauge.core.port.repository.SettingRepository;
import com.knowgauge.core.service.ingestion.IngestionJobProperties;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.settings.SystemSettingsService.SystemSettingValue;
import com.knowgauge.core.service.testgeneration.TestGenerationDefaultsProperties;
import com.knowgauge.core.support.SettingConfigs;

/** System values: SYSTEM rows override the properties files and are applied to the properties beans. */
class SystemSettingsServiceImplTest {

	private static final String LIMIT = TenantLimits.MONTHLY_TOKENS;
	private static final String MODEL = "kg.testgen.defaults.generation-model";
	private static final String BATCH = "kg.testgen.defaults.question-generation-batch-size";

	private final SettingRepository repository = mock(SettingRepository.class);
	private final TenantLimitProperties limits = new TenantLimitProperties();
	private final TestGenerationDefaultsProperties testgen = new TestGenerationDefaultsProperties();
	private SettingConfigRepository configs;
	private CoreSystemSettings core;
	private SystemSettingsServiceImpl service;

	private static Setting row(String key, String value) {
		Setting row = Setting.system(key, value);
		row.setId(1L);
		return row;
	}

	/** The properties files: a limit of 1,000 tokens, model gpt-a, batches of 5. */
	@BeforeEach
	void setUp() {
		limits.setMonthlyTokenLimit(1_000L);
		testgen.setGenerationModel("gpt-a");
		testgen.setQuestionGenerationBatchSize(5);
		testgen.setQuestionCount(10);
		testgen.setDifficulty(TestDifficulty.MEDIUM);
		core = new CoreSystemSettings(limits, testgen, new InvitationProperties(), new IngestionJobProperties(),
				mock(TokenPriceCatalog.class));
		// setting_configs / setting_config_owners as Flyway seeds them.
		configs = SettingConfigs.seededFor(core.definitions());
		service = new SystemSettingsServiceImpl(repository, configs, List.of(core));
	}

	private SystemSettingValue setting(String key) {
		return service.list().stream().filter(s -> s.definition().key().equals(key)).findFirst().orElseThrow();
	}

	@Test
	void atStartupSavedValuesOverrideThePropertiesAndKeepTheirDefaults() {
		when(repository.findSystem()).thenReturn(List.of(row(MODEL, "gpt-b"), row(LIMIT, "0")));

		service.afterSingletonsInstantiated();

		assertThat(testgen.getGenerationModel()).isEqualTo("gpt-b");
		assertThat(limits.getMonthlyTokenLimit()).isZero();
		assertThat(setting(MODEL)).extracting(SystemSettingValue::defaultValue, SystemSettingValue::overridden,
				SystemSettingValue::value).containsExactly("gpt-a", true, "gpt-b");
		assertThat(setting(LIMIT)).extracting(SystemSettingValue::defaultValue, SystemSettingValue::overridden,
				SystemSettingValue::value).containsExactly("1000", true, "0");
		assertThat(setting(BATCH)).extracting(SystemSettingValue::overridden, SystemSettingValue::value)
				.containsExactly(false, "5");
	}

	@Test
	void invalidOrUnknownSavedValuesAreIgnored() {
		when(repository.findSystem()).thenReturn(List.of(row(BATCH, "99"), row("kg.unknown", "x"), row(LIMIT, "")));

		service.afterSingletonsInstantiated();

		assertThat(testgen.getQuestionGenerationBatchSize()).isEqualTo(5);
		assertThat(setting(BATCH).overridden()).isFalse();
		// A limit needs a value now: "no limit" is 0 (ADR014).
		assertThat(limits.getMonthlyTokenLimit()).isEqualTo(1_000L);
	}

	@Test
	void saveStoresAllValuesTogetherAsSystemRowsAndAppliesThemAtOnce() {
		when(repository.findSystem()).thenReturn(List.of());
		service.afterSingletonsInstantiated();
		when(repository.findSystem(any())).thenReturn(Optional.empty());

		service.setAll(Map.of(BATCH, " 8 ", LIMIT, "0"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Setting>> saved = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(saved.capture());
		assertThat(saved.getValue()).extracting(Setting::getOwner, Setting::getOwnerId, Setting::getKey,
				Setting::getValue).containsExactlyInAnyOrder(tuple(SettingOwner.SYSTEM, null, BATCH, "8"),
						tuple(SettingOwner.SYSTEM, null, LIMIT, "0"));
		assertThat(testgen.getQuestionGenerationBatchSize()).isEqualTo(8);
		assertThat(limits.getMonthlyTokenLimit()).isZero();
		assertThat(setting(LIMIT).overridden()).isTrue();
	}

	@Test
	void oneWrongValueSavesNothing() {
		when(repository.findSystem()).thenReturn(List.of());
		service.afterSingletonsInstantiated();

		assertThatThrownBy(() -> service.setAll(Map.of(MODEL, "gpt-b", BATCH, "0")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 1 and 20");
		verify(repository, never()).saveAll(any());
		assertThat(testgen.getGenerationModel()).isEqualTo("gpt-a");
	}

	@Test
	void resetRemovesEverySystemRowAndRestoresTheProperties() {
		when(repository.findSystem()).thenReturn(List.of(row(MODEL, "gpt-b"), row(LIMIT, "0")));
		service.afterSingletonsInstantiated();

		service.resetAll();

		verify(repository).deleteSystem();
		assertThat(testgen.getGenerationModel()).isEqualTo("gpt-a");
		assertThat(limits.getMonthlyTokenLimit()).isEqualTo(1_000L);
		assertThat(service.list()).noneMatch(SystemSettingValue::overridden);
	}

	@Test
	void valuesOutsideTheRulesAndUnknownKeysAreRefused() {
		when(repository.findSystem()).thenReturn(List.of());
		service.afterSingletonsInstantiated();

		assertThatThrownBy(() -> service.setAll(Map.of(MODEL, " "))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.setAll(Map.of(LIMIT, ""))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.setAll(Map.of("kg.testgen.defaults.difficulty", "EXTREME")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.setAll(Map.of("kg.unknown", "1")))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(repository, never()).saveAll(any());
	}

	@Test
	void theOwnersComeFromTheDatabase() {
		service.afterSingletonsInstantiated();

		List<String> perTenant = new java.util.ArrayList<>(TenantLimits.KEYS);
		perTenant.addAll(SettingConfigs.WORKSPACE_CONFIG);
		// Only the core settings are defined here; the answer checking model comes from the testgeneration module.
		perTenant.remove(ConfigKeys.VERIFICATION_MODEL);
		assertThat(service.definitions(SettingOwner.TENANT)).extracting(SettingDefinition::key)
				.containsExactlyInAnyOrderElementsOf(perTenant);
		assertThat(service.config("kg.ingestion.job.batch-size").allows(SettingOwner.TENANT)).isFalse();
		assertThat(service.definitions(SettingOwner.SYSTEM)).hasSameSizeAs(service.definitions());
		assertThat(service.config(SettingConfigs.INVITATIONS_AT_ONCE).getType()).isEqualTo(SettingType.LIMIT);
		assertThat(service.config(SettingConfigs.INVITATIONS_AT_ONCE).allows(SettingOwner.TENANT)).isFalse();
		assertThat(setting(MODEL).config().getType()).isEqualTo(SettingType.CONFIG);
	}

	@Test
	void aSettingWithoutASystemOwnerIsNotOnThePageAndCannotBeSaved() {
		// The database allows only workspace values of the batch size.
		configs = SettingConfigs.returning(core.definitions().stream()
				.map(d -> d.key().equals(BATCH)
						? new SettingConfig(BATCH, SettingType.CONFIG, EnumSet.of(SettingOwner.TENANT))
						: SettingConfigs.seeded(d))
				.toList());
		service = new SystemSettingsServiceImpl(repository, configs, List.of(core));
		when(repository.findSystem()).thenReturn(List.of(row(BATCH, "8")));

		service.afterSingletonsInstantiated();

		assertThat(testgen.getQuestionGenerationBatchSize()).isEqualTo(5);
		assertThat(service.list()).extracting(s -> s.definition().key()).doesNotContain(BATCH);
		assertThat(service.definitions(SettingOwner.TENANT)).extracting(SettingDefinition::key).contains(BATCH);
		assertThatThrownBy(() -> service.setAll(Map.of(BATCH, "8"))).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("system-wide");
		verify(repository, never()).saveAll(any());
	}

	@Test
	void startupFailsWhenADefinedKeyIsMissingFromSettingConfigs() {
		configs = SettingConfigs.returning(List.of(SettingConfigs.seeded(core.definitions().get(0))));
		service = new SystemSettingsServiceImpl(repository, configs, List.of(core));

		assertThatThrownBy(service::afterSingletonsInstantiated).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("without a row in setting_configs").hasMessageContaining(BATCH)
				.hasMessageContaining(MODEL);
		verify(repository, never()).findSystem();
	}
}
