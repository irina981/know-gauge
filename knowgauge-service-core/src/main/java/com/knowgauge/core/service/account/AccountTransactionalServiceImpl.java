package com.knowgauge.core.service.account;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/** Short transactions of {@link AccountServiceImpl}. */
@Slf4j
@Service
public class AccountTransactionalServiceImpl {

	private static final String DEFAULT_TENANT_NAME = "Personal workspace";

	private final UserRepository userRepository;
	private final TenantRepository tenantRepository;

	public AccountTransactionalServiceImpl(UserRepository userRepository, TenantRepository tenantRepository) {
		this.userRepository = userRepository;
		this.tenantRepository = tenantRepository;
	}

	@Transactional
	public User signIn(String externalId, String email, String displayName) {
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
			return userRepository.save(user);
		}
		return createWithPersonalTenant(externalId, email, displayName, now);
	}

	@Transactional(readOnly = true)
	public Optional<Tenant> findTenant(Long tenantId) {
		return tenantRepository.findById(tenantId);
	}

	private User createWithPersonalTenant(String externalId, String email, String displayName, Instant now) {
		String tenantName = displayName != null ? displayName : email != null ? email : DEFAULT_TENANT_NAME;
		Tenant tenant = tenantRepository
				.save(Tenant.builder().type(TenantType.INDIVIDUAL).name(tenantName).build());
		// Every user administers their own tenant for now; more roles come with organization tenants.
		User user = userRepository.save(User.builder().externalId(externalId).tenantId(tenant.getId())
				.role(Role.TENANT_ADMIN).email(email).displayName(displayName).lastSeenAt(now).build());
		tenant.setOwnerUserId(user.getId());
		tenantRepository.save(tenant);
		log.info("First sign-in: user {} created with personal tenant {}", user.getId(), tenant.getId());
		return user;
	}
}
