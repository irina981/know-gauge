package com.knowgauge.core.service.account;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.UserPreferences;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;

import lombok.extern.slf4j.Slf4j;

/** Short transactions of {@link AccountServiceImpl}. */
@Slf4j
@Service
public class AccountTransactionalServiceImpl {

	private static final String DEFAULT_TENANT_NAME = "Personal workspace";

	private final UserRepository userRepository;
	private final TenantRepository tenantRepository;
	private final AccountProperties properties;
	private final TenantSettingsService tenantSettings;

	public AccountTransactionalServiceImpl(UserRepository userRepository, TenantRepository tenantRepository,
			AccountProperties properties, TenantSettingsService tenantSettings) {
		this.userRepository = userRepository;
		this.tenantRepository = tenantRepository;
		this.properties = properties;
		this.tenantSettings = tenantSettings;
	}

	@Transactional
	public User signIn(String externalId, String email, boolean emailVerified, String displayName) {
		Instant now = Instant.now();
		Optional<User> existing = userRepository.findByExternalId(externalId);
		if (existing.isPresent()) {
			User user = existing.get();
			// The provider owns these: keep them as it reports them now.
			if (email != null && !Objects.equals(email, user.getEmail())) {
				user.setEmail(email);
			}
			if (displayName != null && !Objects.equals(displayName, user.getDisplayName())) {
				user.setDisplayName(displayName);
			}
			user.setLastSeenAt(now);
			boolean madeAdmin = grantAdminIfListed(user, emailVerified);
			User saved = userRepository.save(user);
			if (madeAdmin) {
				giveAdminTenantNoLimits(saved);
			}
			return saved;
		}
		return createWithPersonalTenant(externalId, email, emailVerified, displayName, now);
	}

	@Transactional(readOnly = true)
	public Optional<Tenant> findTenant(Long tenantId) {
		return tenantRepository.findById(tenantId);
	}

	@Transactional(readOnly = true)
	public UserPreferences getPreferences(Long userId) {
		if (userId == null) {
			return UserPreferences.defaults();
		}
		return userRepository.findById(userId).map(User::getPreferences).orElseGet(UserPreferences::defaults);
	}

	@Transactional
	public UserPreferences updatePreferences(Long userId, UserPreferences preferences) {
		User user = (userId == null ? Optional.<User>empty() : userRepository.findById(userId))
				.orElseThrow(() -> new ResourceNotFoundException("No stored user to keep the settings for."));
		user.setPreferences(preferences);
		return userRepository.save(user).getPreferences();
	}

	private User createWithPersonalTenant(String externalId, String email, boolean emailVerified, String displayName,
			Instant now) {
		String tenantName = displayName != null ? displayName : email != null ? email : DEFAULT_TENANT_NAME;
		Tenant tenant = tenantRepository
				.save(Tenant.builder().type(TenantType.PERSONAL).name(tenantName).build());
		// Every user administers their own tenant for now; more roles come with organization tenants.
		User newUser = User.builder().externalId(externalId).tenantId(tenant.getId())
				.roles(EnumSet.of(Role.TENANT_ADMIN)).email(email).displayName(displayName).lastSeenAt(now).build();
		boolean madeAdmin = grantAdminIfListed(newUser, emailVerified);
		User user = userRepository.save(newUser);
		tenant.setOwnerUserId(user.getId());
		tenantRepository.save(tenant);
		if (madeAdmin) {
			giveAdminTenantNoLimits(user);
		}
		log.info("First sign-in: user {} created with personal tenant {}", user.getId(), tenant.getId());
		return user;
	}

	/**
	 * Makes the first application admins: a verified email listed in {@code kg.account.admin-emails} gets ADMIN.
	 * Only adds; an admin keeps the role when their email leaves the list.
	 *
	 * @return whether the user got the role now
	 */
	private boolean grantAdminIfListed(User user, boolean emailVerified) {
		if (emailVerified && !user.hasRole(Role.ADMIN) && properties.isAdminEmail(user.getEmail())) {
			Set<Role> roles = EnumSet.noneOf(Role.class);
			if (user.getRoles() != null) {
				roles.addAll(user.getRoles());
			}
			roles.add(Role.ADMIN);
			user.setRoles(roles);
			log.info("Subject {} granted ADMIN: their verified email is in kg.account.admin-emails", user.getExternalId());
			return true;
		}
		return false;
	}

	/**
	 * An application admin's own tenant has no limits (ADR014): its own value 0 for each, which an admin may change
	 * later on the Workspaces page.
	 */
	private void giveAdminTenantNoLimits(User admin) {
		tenantSettings.setUnlimited(admin.getTenantId());
		log.info("Tenant {} of admin user {} set to no limits", admin.getTenantId(), admin.getId());
	}
}
