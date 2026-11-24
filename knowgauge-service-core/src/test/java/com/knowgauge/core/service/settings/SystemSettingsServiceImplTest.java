package com.knowgauge.core.service.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

/** System values: SYSTEM rows override the properties files and are applied to the properties beans. */
class SystemSettingsServiceImplTest {

	private static final String LIMIT = TenantLimits.MONTHLY_TOKENS;
	private static final String MODEL = "kg.testgen.defaults.generation-model";
	private static final String BATCH = "kg.testgen.defaults.question-generation-batch-size";

	private final SettingRepository repository = mock(SettingRepository.class);
	private final SettingConfigRepository configs = mock(SettingConfigRepository.class);
	private final TenantLimitProperties limits = new TenantLimitProperties();
	private final TestGenerationDefaultsProperties testgen = new TestGenerationDefaultsProperties();
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
		CoreSystemSettings core = new CoreSystemSettings(limits, testgen, new InvitationProperties(),
				new IngestionJobProperties(), mock(TokenPriceCatalog.class));
		// setting_configs as Flyway seeds it: every defined key with its type.
		when(configs.findAll()).thenReturn(core.definitions().stream()
				.map(d -> new SettingConfig(d.key(), d.settingType())).toList());
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
	void onlyTheTenantLimitsAreTenantLevel() {
		assertThat(service.definitions()).filteredOn(SettingDefinition::tenantLevel)
				.extracting(SettingDefinition::key).containsExactlyElementsOf(TenantLimits.KEYS);
	}

	@Test
	void theLimitsAreTheTenantLimitsAndTheInvitationsSentAtOnce() {
		List<String> limitKeys = new java.util.ArrayList<>(TenantLimits.KEYS);
		limitKeys.add("kg.invitations.max-per-request");
		assertThat(service.definitions()).filteredOn(d -> d.settingType() == SettingType.LIMIT)
				.extracting(SettingDefinition::key).containsExactlyElementsOf(limitKeys);
		// A system-wide limit: not in a workspace's Limits dialog.
		assertThat(service.definitions()).filteredOn(d -> d.key().equals("kg.invitations.max-per-request"))
				.extracting(SettingDefinition::tenantLevel).containsExactly(false);
	}

	@Test
	void startupFailsWhenADefinedKeyIsMissingFromSettingConfigsOrHasAnotherType() {
		when(configs.findAll()).thenReturn(List.of(new SettingConfig(MODEL, SettingType.LIMIT)));

		assertThatThrownBy(service::afterSingletonsInstantiated).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(BATCH + " is not in setting_configs")
				.hasMessageContaining(MODEL + " is LIMIT in setting_configs but CONFIG");
		verify(repository, never()).findSystem();
	}
}
