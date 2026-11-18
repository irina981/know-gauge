package com.knowgauge.core.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;

class AccountServiceImplTest {

	private static final String SUBJECT = "6f1c2a3b-4d5e-4f60-8a71-b2c3d4e5f601";

	private UserRepository userRepository;
	private TenantRepository tenantRepository;
	private AccountServiceImpl service;
	private final AtomicLong ids = new AtomicLong(10);

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		tenantRepository = mock(TenantRepository.class);
		when(userRepository.findByExternalId(any())).thenReturn(Optional.empty());
		when(userRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0, User.class)));
		when(tenantRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0, Tenant.class)));
		service = new AccountServiceImpl(new AccountTransactionalServiceImpl(userRepository, tenantRepository));
	}

	@Test
	void firstSignInCreatesThePersonalTenantAndTheUserAsItsTenantAdmin() {
		User user = service.signIn(SUBJECT, "ana@example.com", "  Ana   Petrovic ");

		assertThat(user.getId()).isNotNull();
		assertThat(user.getExternalId()).isEqualTo(SUBJECT);
		assertThat(user.getRole()).isEqualTo(Role.TENANT_ADMIN);
		assertThat(user.getDisplayName()).isEqualTo("Ana Petrovic");
		assertThat(user.getLastSeenAt()).isNotNull();
		// saved twice: created, then linked to its owner
		verify(tenantRepository, times(2)).save(any());
		Tenant tenant = lastSavedTenant();
		assertThat(tenant.getId()).isEqualTo(user.getTenantId());
		assertThat(tenant.getType()).isEqualTo(TenantType.INDIVIDUAL);
		assertThat(tenant.getName()).isEqualTo("Ana Petrovic");
		assertThat(tenant.getOwnerUserId()).isEqualTo(user.getId());
	}

	@Test
	void tenantIsNamedAfterTheEmailWhenTheTokenHasNoName() {
		service.signIn(SUBJECT, "ana@example.com", null);

		assertThat(lastSavedTenant().getName()).isEqualTo("ana@example.com");
	}

	@Test
	void laterSignInKeepsTheTenantAndTakesTheNewEmail() {
		User existing = User.builder().id(5L).externalId(SUBJECT).tenantId(7L).role(Role.TENANT_ADMIN)
				.email("old@example.com").displayName("Ana").build();
		when(userRepository.findByExternalId(SUBJECT)).thenReturn(Optional.of(existing));

		User user = service.signIn(SUBJECT, "new@example.com", "Ana");

		assertThat(user.getTenantId()).isEqualTo(7L);
		assertThat(user.getEmail()).isEqualTo("new@example.com");
		verify(tenantRepository, never()).save(any());
	}

	@Test
	void concurrentFirstSignInReadsTheUserTheOtherRequestCreated() {
		User created = User.builder().id(5L).externalId(SUBJECT).tenantId(7L).role(Role.TENANT_ADMIN).build();
		when(userRepository.findByExternalId(SUBJECT)).thenReturn(Optional.empty(), Optional.of(created));
		org.mockito.Mockito.doThrow(new DataIntegrityViolationException("uk_users_external_id"))
				.doAnswer(inv -> inv.getArgument(0)).when(userRepository).save(any());

		User user = service.signIn(SUBJECT, null, null);

		assertThat(user.getId()).isEqualTo(5L);
		assertThat(user.getTenantId()).isEqualTo(7L);
	}

	@Test
	void rejectsAMissingOrReservedSubject() {
		assertThatThrownBy(() -> service.signIn(" ", null, null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.signIn("system", null, null)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reserved");
		verify(userRepository, never()).save(any());
	}

	private Tenant lastSavedTenant() {
		org.mockito.ArgumentCaptor<Tenant> captor = org.mockito.ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
		return captor.getValue();
	}

	private <T extends com.knowgauge.core.model.PersistedObject> T withId(T object) {
		if (object.getId() == null) {
			object.setId(ids.incrementAndGet());
		}
		return object;
	}
}
