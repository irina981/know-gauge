package com.knowgauge.core.service.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.knowgauge.core.model.SystemSetting;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.port.repository.SystemSettingRepository;
import com.knowgauge.core.service.ingestion.IngestionJobProperties;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.settings.SystemSettingsService.SystemSettingValue;
import com.knowgauge.core.service.testgeneration.TestGenerationDefaultsProperties;

class SystemSettingsServiceImplTest {

	private static final String LIMIT = CoreSystemSettings.MONTHLY_TOKEN_LIMIT;
	private static final String MODEL = "kg.testgen.defaults.generation-model";
	private static final String BATCH = "kg.testgen.defaults.question-generation-batch-size";

	private final SystemSettingRepository repository = mock(SystemSettingRepository.class);
	private final TenantLimitProperties limits = new TenantLimitProperties();
	private final TestGenerationDefaultsProperties testgen = new TestGenerationDefaultsProperties();
	private SystemSettingsServiceImpl service;

	private static SystemSetting row(String key, String value) {
		return SystemSetting.builder().id(1L).key(key).value(value).build();
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
		service = new SystemSettingsServiceImpl(repository, List.of(core), limits);
	}

	private SystemSettingValue setting(String key) {
		return service.list().stream().filter(s -> s.definition().key().equals(key)).findFirst().orElseThrow();
	}

	@Test
	void atStartupSavedValuesOverrideThePropertiesAndKeepTheirDefaults() {
		when(repository.findAll()).thenReturn(List.of(row(MODEL, "gpt-b"), row(LIMIT, "")));

		service.afterSingletonsInstantiated();

		assertThat(testgen.getGenerationModel()).isEqualTo("gpt-b");
		assertThat(service.monthlyTokenLimit()).isEmpty();
		assertThat(setting(MODEL)).extracting(SystemSettingValue::defaultValue, SystemSettingValue::overridden,
				SystemSettingValue::value).containsExactly("gpt-a", true, "gpt-b");
		assertThat(setting(LIMIT)).extracting(SystemSettingValue::defaultValue, SystemSettingValue::overridden,
				SystemSettingValue::value).containsExactly("1000", true, null);
		assertThat(setting(BATCH)).extracting(SystemSettingValue::overridden, SystemSettingValue::value)
				.containsExactly(false, "5");
	}

	@Test
	void invalidOrUnknownSavedValuesAreIgnored() {
		when(repository.findAll()).thenReturn(List.of(row(BATCH, "99"), row("kg.unknown", "x")));

		service.afterSingletonsInstantiated();

		assertThat(testgen.getQuestionGenerationBatchSize()).isEqualTo(5);
		assertThat(setting(BATCH).overridden()).isFalse();
	}

	@Test
	void saveStoresAllValuesTogetherAndAppliesThemAtOnce() {
		when(repository.findAll()).thenReturn(List.of());
		service.afterSingletonsInstantiated();
		when(repository.findByKey(any())).thenReturn(Optional.empty());

		service.setAll(Map.of(BATCH, " 8 ", LIMIT, ""));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<SystemSetting>> saved = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(saved.capture());
		assertThat(saved.getValue()).extracting(SystemSetting::getKey, SystemSetting::getValue)
				.containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(BATCH, "8"),
						org.assertj.core.groups.Tuple.tuple(LIMIT, ""));
		assertThat(testgen.getQuestionGenerationBatchSize()).isEqualTo(8);
		assertThat(service.monthlyTokenLimit()).isEmpty();
		assertThat(setting(LIMIT).overridden()).isTrue();
	}

	@Test
	void oneWrongValueSavesNothing() {
		when(repository.findAll()).thenReturn(List.of());
		service.afterSingletonsInstantiated();

		assertThatThrownBy(() -> service.setAll(Map.of(MODEL, "gpt-b", BATCH, "0")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 1 and 20");
		verify(repository, never()).saveAll(any());
		assertThat(testgen.getGenerationModel()).isEqualTo("gpt-a");
	}

	@Test
	void resetRemovesEveryOverrideAndRestoresTheProperties() {
		when(repository.findAll()).thenReturn(List.of(row(MODEL, "gpt-b"), row(LIMIT, "")));
		service.afterSingletonsInstantiated();

		service.resetAll();

		verify(repository).deleteAll();
		assertThat(testgen.getGenerationModel()).isEqualTo("gpt-a");
		assertThat(service.monthlyTokenLimit()).contains(1_000L);
		assertThat(service.list()).noneMatch(SystemSettingValue::overridden);
	}

	@Test
	void valuesOutsideTheRulesAndUnknownKeysAreRefused() {
		when(repository.findAll()).thenReturn(List.of());
		service.afterSingletonsInstantiated();

		assertThatThrownBy(() -> service.setAll(Map.of(MODEL, " "))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.setAll(Map.of("kg.testgen.defaults.difficulty", "EXTREME")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.setAll(Map.of("kg.unknown", "1")))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(repository, never()).saveAll(any());
	}
}
