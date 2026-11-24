package com.knowgauge.core.service.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Setting;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.enums.SettingOwner;
import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.port.repository.SettingRepository;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.service.ingestion.IngestionJobProperties;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.settings.TenantSettingsService.TenantSettingValue;
import com.knowgauge.core.service.testgeneration.TestGenerationDefaultsProperties;
import com.knowgauge.core.support.SettingConfigs;

/** A tenant's own values override the system values (ADR014); 0 means no limit. */
class TenantSettingsServiceImplTest {

	private static final long TENANT = 5L;
	private static final long OTHER = 6L;
	private static final String TOKENS = TenantLimits.MONTHLY_TOKENS;
	private static final String STORAGE = TenantLimits.STORAGE_MB;
	private static final String CONCURRENT = TenantLimits.CONCURRENT_GENERATIONS;
	private static final String PARTICIPANTS = TenantLimits.PARTICIPANTS_PER_TEST;

	private final SettingRepository repository = mock(SettingRepository.class);
	private final TenantRepository tenants = mock(TenantRepository.class);
	private TenantSettingsServiceImpl service;

	/** The system values: 1,000 tokens, 10 MB, 1 generation at once, 100 participants per test. */
	@BeforeEach
	void setUp() {
		TenantLimitProperties limits = new TenantLimitProperties();
		limits.setMonthlyTokenLimit(1_000L);
		limits.setStorageLimitMb(10L);
		limits.setConcurrentGenerationsPerUser(1);
		limits.setParticipantsPerTest(100);
		CoreSystemSettings core = new CoreSystemSettings(limits, new TestGenerationDefaultsProperties(),
				new InvitationProperties(), new IngestionJobProperties(), mock(TokenPriceCatalog.class));
		// The tenant-level settings are those with a TENANT owner, as Flyway seeds setting_config_owners.
		SystemSettingsServiceImpl systemSettings = new SystemSettingsServiceImpl(mock(SettingRepository.class),
				SettingConfigs.seededFor(core.definitions()), List.of(core));
		systemSettings.afterSingletonsInstantiated();
		service = new TenantSettingsServiceImpl(repository, tenants, systemSettings);
		when(tenants.findById(TENANT))
				.thenReturn(Optional.of(Tenant.builder().id(TENANT).type(TenantType.PERSONAL).build()));
		when(repository.findByTenantId(TENANT)).thenReturn(List.of());
	}

	private static Setting own(long tenantId, String key, String value) {
		Setting row = Setting.tenant(tenantId, key, value);
		row.setId(tenantId * 100 + key.length());
		return row;
	}

	@Test
	void withoutValuesOfItsOwnATenantHasTheSystemLimits() {
		assertThat(service.limits(TENANT)).isEqualTo(new TenantLimits(1_000L, 10L * 1024 * 1024, 1, 100));
		assertThat(service.list(TENANT)).filteredOn(v -> v.config().getType() == SettingType.LIMIT)
				.extracting(TenantSettingValue::definition).extracting(SettingDefinition::key)
				.containsExactly(TOKENS, STORAGE, CONCURRENT, PARTICIPANTS);
		// The configuration a workspace may set too (V10); the ingestion batch size is system-only.
		assertThat(service.list(TENANT)).filteredOn(v -> v.config().getType() == SettingType.CONFIG)
				.extracting(v -> v.definition().key()).containsExactly(ConfigKeys.GENERATION_MODEL,
						ConfigKeys.QUESTIONS_PER_CALL, ConfigKeys.QUESTION_COUNT, ConfigKeys.DIFFICULTY);
		assertThat(service.list(TENANT)).noneMatch(TenantSettingValue::overridden);
	}

	@Test
	void aValueOfItsOwnWinsAndZeroMeansNoLimit() {
		when(repository.findByTenantId(TENANT)).thenReturn(List.of(own(TENANT, TOKENS, "5000"), own(TENANT, STORAGE, "0")));

		assertThat(service.limits(TENANT)).isEqualTo(new TenantLimits(5_000L, null, 1, 100));
		assertThat(service.list(TENANT)).filteredOn(v -> v.config().getType() == SettingType.LIMIT)
				.extracting(TenantSettingValue::systemValue, TenantSettingValue::ownValue, TenantSettingValue::value)
				.containsExactly(tuple("1000", "5000", "5000"), tuple("10", "0", "0"),
						tuple("1", null, "1"), tuple("100", null, "100"));
	}

	@Test
	void limitsOfSeveralTenantsAreReadAtOnce() {
		when(repository.findByTenantIdIn(List.of(TENANT, OTHER))).thenReturn(List.of(own(OTHER, CONCURRENT, "3")));

		Map<Long, TenantLimits> limits = service.limits(List.of(TENANT, OTHER));

		assertThat(limits.get(TENANT).concurrentGenerations()).isEqualTo(1);
		assertThat(limits.get(OTHER).concurrentGenerations()).isEqualTo(3);
		assertThat(service.withOwnValues(List.of(TENANT, OTHER))).containsExactly(OTHER);
	}

	@Test
	void ownConfigurationValuesAreReadButDoNotCountAsOwnLimits() {
		when(repository.findByTenantId(TENANT)).thenReturn(List.of(own(TENANT, ConfigKeys.DIFFICULTY, "HARD"),
				own(TENANT, ConfigKeys.QUESTION_COUNT, "999"), own(TENANT, TOKENS, "5000")));
		when(repository.findByTenantIdIn(List.of(TENANT))).thenReturn(List.of(own(TENANT, ConfigKeys.DIFFICULTY, "HARD")));

		// 999 questions is over the allowed 100, so that stored value is left out.
		assertThat(service.ownValues(TENANT)).containsExactlyInAnyOrderEntriesOf(
				Map.of(ConfigKeys.DIFFICULTY, "HARD", TOKENS, "5000"));
		assertThat(service.withOwnValues(List.of(TENANT))).isEmpty();
	}

	@Test
	void aWorkspaceMaySaveItsOwnConfiguration() {
		service.setAll(TENANT, Map.of(ConfigKeys.DIFFICULTY, "hard"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Setting>> saved = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(saved.capture());
		assertThat(saved.getValue()).extracting(Setting::getOwner, Setting::getOwnerId, Setting::getKey, Setting::getValue)
				.containsExactly(tuple(SettingOwner.TENANT, TENANT, ConfigKeys.DIFFICULTY, "HARD"));
	}

	@Test
	void saveAddsChangesAndRemovesValuesOfItsOwn() {
		Setting tokens = own(TENANT, TOKENS, "5000");
		Setting storage = own(TENANT, STORAGE, "20");
		when(repository.findByTenantId(TENANT)).thenReturn(List.of(tokens, storage));
		Map<String, String> values = new LinkedHashMap<>();
		values.put(TOKENS, " 7000 ");
		values.put(STORAGE, "");
		values.put(CONCURRENT, "0");

		service.setAll(TENANT, values);

		verify(repository).deleteAll(List.of(storage));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Setting>> saved = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(saved.capture());
		assertThat(saved.getValue()).extracting(Setting::getOwner, Setting::getOwnerId, Setting::getKey,
				Setting::getValue).containsExactly(tuple(SettingOwner.TENANT, TENANT, TOKENS, "7000"),
						tuple(SettingOwner.TENANT, TENANT, CONCURRENT, "0"));
	}

	@Test
	void setUnlimitedStoresZeroForEveryLimit() {
		service.setUnlimited(TENANT);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Setting>> saved = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(saved.capture());
		assertThat(saved.getValue()).extracting(Setting::getKey, Setting::getValue)
				.containsExactly(tuple(TOKENS, "0"), tuple(STORAGE, "0"), tuple(CONCURRENT, "0"),
						tuple(PARTICIPANTS, "0"));
	}

	@Test
	void wrongValuesSystemOnlyKeysAndUnknownTenantsAreRefused() {
		assertThatThrownBy(() -> service.setAll(TENANT, Map.of(TOKENS, "-1", CONCURRENT, "99")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least 0")
				.hasMessageContaining("between 0 and 20");
		assertThatThrownBy(() -> service.setAll(TENANT, Map.of("kg.ingestion.job.batch-size", "5")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cannot have a value per workspace");
		assertThatThrownBy(() -> service.setAll(TENANT, Map.of("kg.unknown", "1")))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> service.list(99L)).isInstanceOf(ResourceNotFoundException.class);
		verify(repository, never()).saveAll(anyList());
		verify(repository, never()).deleteAll(any());
	}

	@Test
	void resetRemovesTheTenantsOwnValues() {
		service.resetAll(TENANT);

		verify(repository).deleteByTenantId(TENANT);
	}
}
