package com.knowgauge.core.service.tenant;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.context.TenantViewProvider;
import com.knowgauge.core.exception.TenantAccessDeniedException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.SettingType;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.core.service.settings.TenantSettingsService.TenantSettingValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrganizationTenantServiceImpl implements OrganizationTenantService {

	/** By name, else email; users with neither come last. */
	private static final Comparator<User> BY_NAME = Comparator.comparing(
			(User u) -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail(),
			Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

	private final ExecutionContext executionContext;
	private final TenantViewProvider tenantView;
	private final TenantRepository tenantRepository;
	private final UserRepository userRepository;
	private final TenantSettingsService tenantSettings;
	private final TenantProvisioningService tenantProvisioning;

	public OrganizationTenantServiceImpl(ExecutionContext executionContext, TenantViewProvider tenantView,
			TenantRepository tenantRepository, UserRepository userRepository, TenantSettingsService tenantSettings,
			TenantProvisioningService tenantProvisioning) {
		this.executionContext = executionContext;
		this.tenantView = tenantView;
		this.tenantRepository = tenantRepository;
		this.userRepository = userRepository;
		this.tenantSettings = tenantSettings;
		this.tenantProvisioning = tenantProvisioning;
	}

	@Override
	public List<User> users() {
		long tenantId = requireOrganization();
		return userRepository.findByTenantId(tenantId).stream().sorted(BY_NAME).toList();
	}

	@Override
	public List<TenantInvitation> invitations() {
		return tenantProvisioning.listInvitations(requireOrganization());
	}

	@Override
	public List<TenantInvitation> invite(List<String> emails, Role role, String personalMessage) {
		long tenantId = requireWritableOrganization();
		List<TenantInvitation> invitations = tenantProvisioning.invite(tenantId, emails, role, personalMessage);
		log.info("Tenant {} - user {} invited {} people as {}", tenantId, executionContext.userId(),
				invitations.size(), role);
		return invitations;
	}

	@Override
	public TenantInvitation resendInvitation(Long invitationId) {
		return tenantProvisioning.resend(requireWritableOrganization(), invitationId, null);
	}

	@Override
	public void revokeInvitation(Long invitationId) {
		tenantProvisioning.revoke(requireWritableOrganization(), invitationId);
	}

	@Override
	public List<TenantSettingValue> settings() {
		return configurationOf(tenantSettings.list(requireOrganization()));
	}

	/** Only the configuration: limits are an ADMIN's (later the billing plan's), not shown to tenant admins. */
	private static List<TenantSettingValue> configurationOf(List<TenantSettingValue> settings) {
		return settings.stream().filter(s -> s.config().getType() == SettingType.CONFIG).toList();
	}

	@Override
	public List<TenantSettingValue> saveSettings(Map<String, String> values) {
		long tenantId = requireWritableOrganization();
		Map<String, TenantSettingValue> byKey = new LinkedHashMap<>();
		tenantSettings.list(tenantId).forEach(s -> byKey.put(s.definition().key(), s));
		for (String key : values.keySet()) {
			TenantSettingValue setting = byKey.get(key);
			if (setting != null && setting.config().getType() == SettingType.LIMIT) {
				throw new TenantAccessDeniedException(
						"The limit \"" + setting.definition().label() + "\" is set by an application admin.");
			}
		}
		// Unknown and system-only keys are refused by the settings service.
		List<TenantSettingValue> saved = tenantSettings.setAll(tenantId, values);
		log.info("Configuration of workspace {} changed by user {}: {}", tenantId, executionContext.userId(),
				values.keySet());
		return configurationOf(saved);
	}

	@Override
	public List<TenantSettingValue> resetSettings() {
		long tenantId = requireWritableOrganization();
		Map<String, String> clear = new LinkedHashMap<>();
		tenantSettings.list(tenantId).stream()
				.filter(s -> s.config().getType() == SettingType.CONFIG && s.overridden())
				.forEach(s -> clear.put(s.definition().key(), ""));
		if (clear.isEmpty()) {
			return configurationOf(tenantSettings.list(tenantId));
		}
		log.info("Configuration of workspace {} reset by user {}: {}", tenantId, executionContext.userId(),
				clear.keySet());
		return configurationOf(tenantSettings.setAll(tenantId, clear));
	}

	private long requireWritableOrganization() {
		if (tenantView.isViewingOtherTenant()) {
			throw new TenantAccessDeniedException("Another user's workspace is read-only.");
		}
		return requireOrganization();
	}

	private long requireOrganization() {
		long tenantId = executionContext.tenantId();
		boolean organization = tenantRepository.findById(tenantId).map(Tenant::getType)
				.filter(type -> type == TenantType.ORGANIZATION).isPresent();
		if (!organization) {
			throw new TenantAccessDeniedException(
					"Only an organization workspace has users and settings to manage.");
		}
		return tenantId;
	}
}
