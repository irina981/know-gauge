package com.knowgauge.core.service.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyList;
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

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.context.TenantViewProvider;
import com.knowgauge.core.exception.TenantAccessDeniedException;
import com.knowgauge.core.model.Setting;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantLimits;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.pricing.TokenPriceCatalog;
import com.knowgauge.core.port.repository.SettingRepository;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.core.service.ingestion.IngestionJobProperties;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.settings.ConfigKeys;
import com.knowgauge.core.service.settings.CoreSystemSettings;
import com.knowgauge.core.service.settings.SystemSettingsServiceImpl;
import com.knowgauge.core.service.settings.TenantLimitProperties;
import com.knowgauge.core.service.settings.TenantSettingsServiceImpl;
import com.knowgauge.core.service.testgeneration.TestGenerationDefaultsProperties;
import com.knowgauge.core.support.SettingConfigs;

/** Tenant admins of an organization see its users and change its configuration, never its limits (ADR016). */
class OrganizationTenantServiceImplTest {

	private static final long TENANT = 7L;

	private final ExecutionContext context = mock(ExecutionContext.class);
	private final TenantViewProvider tenantView = mock(TenantViewProvider.class);
	private final TenantRepository tenants = mock(TenantRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final SettingRepository settings = mock(SettingRepository.class);
	private final TenantProvisioningService tenantProvisioning = mock(TenantProvisioningService.class);
	private OrganizationTenantServiceImpl service;

	@BeforeEach
	void setUp() {
		CoreSystemSettings core = new CoreSystemSettings(new TenantLimitProperties(),
				new TestGenerationDefaultsProperties(), new InvitationProperties(), new IngestionJobProperties(),
				mock(TokenPriceCatalog.class));
		SystemSettingsServiceImpl systemSettings = new SystemSettingsServiceImpl(mock(SettingRepository.class),
				SettingConfigs.seededFor(core.definitions()), List.of(core));
		systemSettings.afterSingletonsInstantiated();
		service = new OrganizationTenantServiceImpl(context, tenantView, tenants, users,
				new TenantSettingsServiceImpl(settings, tenants, systemSettings), tenantProvisioning);
		when(context.tenantId()).thenReturn(TENANT);
		tenantIs(TenantType.ORGANIZATION);
		when(settings.findByTenantId(TENANT)).thenReturn(List.of());
	}

	private void tenantIs(TenantType type) {
		when(tenants.findById(TENANT)).thenReturn(Optional.of(Tenant.builder().id(TENANT).type(type).build()));
	}

	private static User user(long id, String name, String email) {
		return User.builder().id(id).tenantId(TENANT).displayName(name).email(email).build();
	}

	@Test
	void usersAreListedByName() {
		when(users.findByTenantId(TENANT)).thenReturn(
				List.of(user(1, "zoe", "z@acme.com"), user(2, null, null), user(3, null, "ana@acme.com"), user(4, "Bob", null)));

		assertThat(service.users()).extracting(User::getId).containsExactly(3L, 4L, 1L, 2L);
	}

	@Test
	void aTenantThatIsNotAnOrganizationIsRefused() {
		tenantIs(TenantType.PERSONAL);

		assertThatThrownBy(service::users).isInstanceOf(TenantAccessDeniedException.class);
		assertThatThrownBy(service::settings).isInstanceOf(TenantAccessDeniedException.class);
		assertThatThrownBy(() -> service.saveSettings(Map.of(ConfigKeys.DIFFICULTY, "HARD")))
				.isInstanceOf(TenantAccessDeniedException.class);
	}

	@Test
	void theConfigurationIsSaved() {
		service.saveSettings(Map.of(ConfigKeys.DIFFICULTY, "hard"));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Setting>> saved = ArgumentCaptor.forClass(List.class);
		verify(settings).saveAll(saved.capture());
		assertThat(saved.getValue()).extracting(Setting::getKey, Setting::getValue)
				.containsExactly(tuple(ConfigKeys.DIFFICULTY, "HARD"));
	}

	@Test
	void aLimitIsNeverSaved() {
		assertThatThrownBy(() -> service.saveSettings(Map.of(ConfigKeys.DIFFICULTY, "HARD",
				TenantLimits.MONTHLY_TOKENS, "0"))).isInstanceOf(TenantAccessDeniedException.class);
		verify(settings, never()).saveAll(anyList());
	}

	@Test
	void nothingIsSavedWhileViewingAnotherTenant() {
		when(tenantView.isViewingOtherTenant()).thenReturn(true);

		assertThatThrownBy(() -> service.saveSettings(Map.of(ConfigKeys.DIFFICULTY, "HARD")))
				.isInstanceOf(TenantAccessDeniedException.class);
		assertThatThrownBy(service::resetSettings).isInstanceOf(TenantAccessDeniedException.class);
	}

	@Test
	void resetRemovesTheOwnConfigurationButKeepsTheLimits() {
		Setting difficulty = Setting.tenant(TENANT, ConfigKeys.DIFFICULTY, "HARD");
		difficulty.setId(1L);
		Setting tokens = Setting.tenant(TENANT, TenantLimits.MONTHLY_TOKENS, "5000");
		tokens.setId(2L);
		when(settings.findByTenantId(TENANT)).thenReturn(List.of(difficulty, tokens));

		service.resetSettings();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<Setting>> removed = ArgumentCaptor.forClass(List.class);
		verify(settings).deleteAll(removed.capture());
		assertThat(removed.getValue()).containsExactly(difficulty);
		verify(settings, never()).deleteByTenantId(TENANT);
	}

	@Test
	void theConfigurationIsListedWithoutTheLimits() {
		assertThat(service.settings()).isNotEmpty()
				.allMatch(s -> s.config().getType() == com.knowgauge.core.model.enums.SettingType.CONFIG);
	}

	@Test
	void invitationsGoToTheCurrentTenantWithTheirRole() {
		service.invite(List.of("ana@acme.com"), Role.TENANT_VIEWER, "Welcome");
		service.revokeInvitation(3L);

		verify(tenantProvisioning).invite(TENANT, List.of("ana@acme.com"), Role.TENANT_VIEWER, "Welcome");
		verify(tenantProvisioning).revoke(TENANT, 3L);
	}

	@Test
	void noOneIsInvitedToATenantThatIsNotAnOrganization() {
		tenantIs(TenantType.PERSONAL);

		assertThatThrownBy(() -> service.invite(List.of("ana@acme.com"), Role.TENANT_USER, null))
				.isInstanceOf(TenantAccessDeniedException.class);
		verify(tenantProvisioning, never()).invite(org.mockito.ArgumentMatchers.anyLong(), anyList(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}
}
