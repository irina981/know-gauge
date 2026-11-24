package com.knowgauge.core.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.exception.WorkspaceInvitationConflictException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.notification.WorkspaceInvitationMailer;
import com.knowgauge.core.port.notification.WorkspaceInvitationMailer.WorkspaceInvitationMail;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.WorkspaceInvitationRepository;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.core.service.content.ContentScope;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.settings.TenantSettingsService;

/** An ADMIN creates workspaces and invites people to them (ADR015). */
class WorkspaceAdminServiceImplTest {

	private TenantRepository tenantRepository;
	private WorkspaceInvitationRepository invitationRepository;
	private TenantSettingsService tenantSettings;
	private WorkspaceInvitationMailer mailer;
	private ContentScope contentScope;
	private WorkspaceAdminServiceImpl service;
	private final AtomicLong ids = new AtomicLong(100);
	private final List<WorkspaceInvitation> stored = new ArrayList<>();

	@BeforeEach
	void setUp() {
		tenantRepository = mock(TenantRepository.class);
		invitationRepository = mock(WorkspaceInvitationRepository.class);
		tenantSettings = mock(TenantSettingsService.class);
		mailer = mock(WorkspaceInvitationMailer.class);
		contentScope = mock(ContentScope.class);
		when(tenantRepository.save(any())).thenAnswer(inv -> {
			Tenant tenant = inv.getArgument(0);
			if (tenant.getId() == null) {
				tenant.setId(ids.incrementAndGet());
			}
			when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
			return tenant;
		});
		when(invitationRepository.save(any())).thenAnswer(inv -> {
			WorkspaceInvitation invitation = inv.getArgument(0);
			if (invitation.getId() == null) {
				invitation.setId(ids.incrementAndGet());
				stored.add(invitation);
			}
			when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
			return invitation;
		});
		when(invitationRepository.findByTenantId(any())).thenAnswer(inv -> stored.stream()
				.filter(i -> i.getTenantId().equals(inv.getArgument(0))).toList());
		InvitationProperties properties = new InvitationProperties();
		properties.setLinkBaseUrl("https://app.example.com/");
		service = new WorkspaceAdminServiceImpl(
				new WorkspaceAdminTransactionalServiceImpl(tenantRepository, invitationRepository, tenantSettings),
				mailer, properties, mock(TenantAdminService.class), contentScope);
	}

	@Test
	void anOrganizationInvitesEveryAddressAsTenantAdmin() {
		service.create(TenantType.ORGANIZATION, "  Acme   Training ", List.of("Ana@Example.com", "marko@example.com",
				"ana@example.com", " "), "Welcome!");

		ArgumentCaptor<Tenant> tenant = ArgumentCaptor.forClass(Tenant.class);
		verify(tenantRepository).save(tenant.capture());
		assertThat(tenant.getValue().getType()).isEqualTo(TenantType.ORGANIZATION);
		assertThat(tenant.getValue().getName()).isEqualTo("Acme Training");
		assertThat(tenant.getValue().getOwnerUserId()).isNull();
		assertThat(stored).extracting(WorkspaceInvitation::getEmail).containsExactly("ana@example.com",
				"marko@example.com");
		assertThat(stored).extracting(WorkspaceInvitation::getRole).containsOnly(Role.TENANT_ADMIN);
		assertThat(stored).allSatisfy(i -> assertThat(i.getSentAt()).isNotNull());

		ArgumentCaptor<WorkspaceInvitationMail> mail = ArgumentCaptor.forClass(WorkspaceInvitationMail.class);
		verify(mailer, org.mockito.Mockito.times(2)).send(mail.capture());
		assertThat(mail.getAllValues().get(0).link())
				.isEqualTo("https://app.example.com/join/" + stored.get(0).getPublicToken());
		assertThat(mail.getAllValues().get(0).personalMessage()).isEqualTo("Welcome!");
		verify(tenantSettings, never()).setUnlimited(any());
	}

	@Test
	void eachTypeNeedsItsNumberOfInvitees() {
		assertThatThrownBy(() -> service.create(TenantType.ORGANIZATION, "Acme", List.of(), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
		assertThatThrownBy(() -> service.create(TenantType.PERSONAL, "Ana", List.of(), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
		assertThatThrownBy(
				() -> service.create(TenantType.PERSONAL, "Ana", List.of("ana@example.com", "b@example.com"), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactly one");
		assertThatThrownBy(() -> service.create(TenantType.LIBRARY, "Samples", List.of("ana@example.com"), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no members");
		assertThatThrownBy(() -> service.create(TenantType.ORGANIZATION, " ", List.of("ana@example.com"), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("name");
		assertThatThrownBy(() -> service.create(TenantType.ORGANIZATION, "Acme", List.of("not-an-email"), null))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not-an-email");
		verify(tenantRepository, never()).save(any());
	}

	@Test
	void aLibraryHasNoLimitsAndIsReadableAtOnce() {
		service.create(TenantType.LIBRARY, "Java samples", null, null);

		verify(tenantSettings).setUnlimited(101L);
		verify(contentScope).refreshLibraries();
		verify(mailer, never()).send(any());
	}

	@Test
	void aFailedSendIsKeptOnTheInvitation() {
		doThrow(new MailDeliveryException("SMTP down", null)).when(mailer).send(any());

		service.create(TenantType.PERSONAL, "Ana", List.of("ana@example.com"), null);

		assertThat(stored.get(0).getSendError()).isEqualTo("SMTP down");
		assertThat(stored.get(0).getSentAt()).isNull();
		assertThat(stored.get(0).getSendCount()).isEqualTo(1);
	}

	@Test
	void aPersonalWorkspaceTakesOneInviteeUntilItHasAnOwner() {
		service.create(TenantType.PERSONAL, "Ana", List.of("ana@example.com"), null);
		Long tenantId = stored.get(0).getTenantId();

		// The same address again keeps its link; another one needs the first revoked.
		service.invite(tenantId, List.of("ana@example.com"), null);
		assertThat(stored).hasSize(1);
		assertThatThrownBy(() -> service.invite(tenantId, List.of("marko@example.com"), null))
				.isInstanceOf(WorkspaceInvitationConflictException.class);

		tenantRepository.findById(tenantId).get().setOwnerUserId(9L);
		assertThatThrownBy(() -> service.invite(tenantId, List.of("ana@example.com"), null))
				.isInstanceOf(WorkspaceInvitationConflictException.class).hasMessageContaining("owner");
	}

	@Test
	void anAcceptedInvitationIsNeitherSentAgainNorRevoked() {
		service.create(TenantType.ORGANIZATION, "Acme", List.of("ana@example.com"), null);
		WorkspaceInvitation accepted = stored.get(0);
		accepted.setAcceptedAt(java.time.Instant.now());
		Long tenantId = accepted.getTenantId();

		List<WorkspaceInvitation> result = service.invite(tenantId, List.of("ana@example.com", "marko@example.com"),
				null);

		assertThat(result).extracting(WorkspaceInvitation::getEmail).containsExactly("ana@example.com",
				"marko@example.com");
		verify(mailer, org.mockito.Mockito.times(2)).send(any()); // create + marko
		assertThatThrownBy(() -> service.resend(tenantId, accepted.getId(), null))
				.isInstanceOf(WorkspaceInvitationConflictException.class);
		assertThatThrownBy(() -> service.revoke(tenantId, accepted.getId()))
				.isInstanceOf(WorkspaceInvitationConflictException.class);
		verify(invitationRepository, never()).delete(any());
	}

	@Test
	void aLibraryTakesNoInvitations() {
		service.create(TenantType.LIBRARY, "Samples", List.of(), null);

		assertThatThrownBy(() -> service.invite(101L, List.of("ana@example.com"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
