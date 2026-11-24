package com.knowgauge.core.service.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.exception.TenantInvitationConflictException;
import com.knowgauge.core.exception.TenantInvitationForbiddenException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.core.port.repository.TenantInvitationRepository;
import com.knowgauge.core.service.tenant.TenantMembershipService.Joined;

/** Accepting a workspace invitation moves the user into the workspace (ADR015). */
class TenantMembershipServiceImplTest {

	private static final long USER = 20L;
	private static final long OLD_TENANT = 5L;
	private static final long NEW_TENANT = 300L;

	private TenantInvitationRepository invitationRepository;
	private TenantRepository tenantRepository;
	private UserRepository userRepository;
	private ExecutionContext context;
	private TenantMembershipServiceImpl service;
	private User user;
	private Tenant workspace;
	private TenantInvitation invitation;

	@BeforeEach
	void setUp() {
		invitationRepository = mock(TenantInvitationRepository.class);
		tenantRepository = mock(TenantRepository.class);
		userRepository = mock(UserRepository.class);
		context = mock(ExecutionContext.class);
		when(context.userId()).thenReturn(OptionalLong.of(USER));
		user = User.builder().id(USER).tenantId(OLD_TENANT).email("Ana@Example.com")
				.roles(EnumSet.of(Role.ADMIN, Role.TENANT_ADMIN)).build();
		when(userRepository.findById(USER)).thenReturn(Optional.of(user));
		when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
		workspace = Tenant.builder().id(NEW_TENANT).type(TenantType.ORGANIZATION).name("Acme").build();
		when(tenantRepository.findById(NEW_TENANT)).thenReturn(Optional.of(workspace));
		invitation = TenantInvitation.builder().id(7L).tenantId(NEW_TENANT).email("ana@example.com")
				.role(Role.TENANT_ADMIN).publicToken("tok").build();
		when(invitationRepository.findByPublicToken("tok")).thenReturn(Optional.of(invitation));
		service = new TenantMembershipServiceImpl(invitationRepository, tenantRepository, userRepository,
				new TenantJoiner(userRepository, tenantRepository, invitationRepository), context);
	}

	@Test
	void acceptingMovesTheUserAndKeepsAnAdminAnAdmin() {
		Joined joined = service.accept("tok", true);

		assertThat(joined.tenantId()).isEqualTo(NEW_TENANT);
		assertThat(joined.tenantName()).isEqualTo("Acme");
		assertThat(joined.previousTenantId()).isEqualTo(OLD_TENANT);
		assertThat(user.getTenantId()).isEqualTo(NEW_TENANT);
		assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.ADMIN, Role.TENANT_ADMIN);
		assertThat(invitation.getAcceptedUserId()).isEqualTo(USER);
		assertThat(invitation.getAcceptedAt()).isNotNull();
		// An organization has no owner.
		verify(tenantRepository, never()).save(any());
	}

	@Test
	void acceptingAPersonalTenantMakesTheUserItsOwner() {
		workspace.setType(TenantType.PERSONAL);

		service.accept("tok", true);

		assertThat(workspace.getOwnerUserId()).isEqualTo(USER);
		verify(tenantRepository).save(workspace);
	}

	@Test
	void aPersonalTenantWithAnotherOwnerCannotBeJoined() {
		workspace.setType(TenantType.PERSONAL);
		workspace.setOwnerUserId(99L);

		assertThatThrownBy(() -> service.accept("tok", true)).isInstanceOf(TenantInvitationConflictException.class);
		assertThat(user.getTenantId()).isEqualTo(OLD_TENANT);
	}

	@Test
	void onlyTheInvitedVerifiedEmailMayAccept() {
		assertThatThrownBy(() -> service.accept("tok", false))
				.isInstanceOf(TenantInvitationForbiddenException.class).hasMessageContaining("Verify");

		user.setEmail("marko@example.com");
		assertThatThrownBy(() -> service.accept("tok", true))
				.isInstanceOf(TenantInvitationForbiddenException.class).hasMessageContaining("ana@example.com");

		verify(userRepository, never()).save(any());
	}

	@Test
	void acceptingAgainIsANoOpButSomeoneElseIsRefused() {
		invitation.setAcceptedAt(Instant.now());
		invitation.setAcceptedUserId(USER);
		assertThat(service.accept("tok", true).previousTenantId()).isNull();

		invitation.setAcceptedUserId(99L);
		assertThatThrownBy(() -> service.accept("tok", true)).isInstanceOf(TenantInvitationConflictException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void anUnknownOrRevokedLinkIsNotFound() {
		when(invitationRepository.findByPublicToken("gone")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.preview("gone")).isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> service.accept("gone", true)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void thePreviewShowsTheTenantAndTheInvitedAddress() {
		var preview = service.preview("tok");

		assertThat(preview.tenantName()).isEqualTo("Acme");
		assertThat(preview.tenantType()).isEqualTo(TenantType.ORGANIZATION);
		assertThat(preview.email()).isEqualTo("ana@example.com");
		assertThat(preview.accepted()).isFalse();
	}
}
