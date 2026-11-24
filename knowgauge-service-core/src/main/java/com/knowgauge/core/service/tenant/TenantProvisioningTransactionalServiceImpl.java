package com.knowgauge.core.service.tenant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.exception.TenantInvitationConflictException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.TenantInvitationRepository;
import com.knowgauge.core.service.settings.TenantSettingsService;
import com.knowgauge.core.util.PublicTokens;

/** Short transactions of {@link TenantProvisioningServiceImpl}; mail goes out between them. */
@Service
public class TenantProvisioningTransactionalServiceImpl {

	/** Invitees get this role in the workspace unless the invitation names another (ADR015, ADR016). */
	static final Role INVITED_ROLE = Role.TENANT_ADMIN;

	/** The workspace and its invitations: {@code toSend} still pending, {@code accepted} left as they are. */
	record Prepared(Tenant tenant, List<TenantInvitation> toSend, List<TenantInvitation> accepted) {
	}

	private final TenantRepository tenantRepository;
	private final TenantInvitationRepository invitationRepository;
	private final TenantSettingsService tenantSettings;

	public TenantProvisioningTransactionalServiceImpl(TenantRepository tenantRepository,
			TenantInvitationRepository invitationRepository, TenantSettingsService tenantSettings) {
		this.tenantRepository = tenantRepository;
		this.invitationRepository = invitationRepository;
		this.tenantSettings = tenantSettings;
	}

	/** @param emails normalized, already checked against the type's rules */
	@Transactional
	public Prepared create(TenantType type, String name, List<String> emails) {
		Tenant tenant = tenantRepository.save(Tenant.builder().type(type).name(name).build());
		if (type == TenantType.LIBRARY) {
			// Libraries have no limits (ADR014): their own value 0 for each.
			tenantSettings.setUnlimited(tenant.getId());
		}
		List<TenantInvitation> invitations = emails.stream()
				.map(email -> invitationRepository.save(newInvitation(tenant.getId(), email, INVITED_ROLE))).toList();
		return new Prepared(tenant, invitations, List.of());
	}

	/**
	 * More invitations: an organization takes any number, a personal workspace one while it has no owner and no
	 * invitation to someone else, a library none. An address invited before keeps its link and gets the new role.
	 *
	 * @param role {@link Role#TENANT_ADMIN} for a personal workspace; any tenant role for an organization
	 */
	@Transactional
	public Prepared prepareInvite(Long tenantId, List<String> emails, Role role) {
		Tenant tenant = requireTenant(tenantId);
		List<TenantInvitation> existing = invitationRepository.findByTenantId(tenantId);
		switch (tenant.getType()) {
		case LIBRARY -> throw new IllegalArgumentException("A shared library has no members, so it takes no invitations.");
		case PERSONAL -> {
			if (role != Role.TENANT_ADMIN) {
				throw new IllegalArgumentException("The owner of a personal workspace is its TENANT_ADMIN.");
			}
			if (emails.size() != 1) {
				throw new IllegalArgumentException("A personal workspace is for one person: invite exactly one address.");
			}
			if (tenant.getOwnerUserId() != null) {
				throw new TenantInvitationConflictException(
						"The personal workspace " + tenant.getName() + " already has its owner.");
			}
			boolean otherPending = existing.stream()
					.anyMatch(invitation -> !invitation.isAccepted() && !invitation.getEmail().equals(emails.get(0)));
			if (otherPending) {
				throw new TenantInvitationConflictException("The personal workspace " + tenant.getName()
						+ " already has an invitation to someone else. Revoke it first.");
			}
		}
		case ORGANIZATION -> {
			if (emails.isEmpty()) {
				throw new IllegalArgumentException("Enter at least one email address.");
			}
		}
		}
		List<TenantInvitation> toSend = new ArrayList<>();
		List<TenantInvitation> accepted = new ArrayList<>();
		for (String email : emails) {
			Optional<TenantInvitation> before = existing.stream().filter(i -> i.getEmail().equals(email)).findFirst();
			if (before.isPresent() && before.get().isAccepted()) {
				accepted.add(before.get());
			} else {
				TenantInvitation invitation = before.orElseGet(() -> newInvitation(tenantId, email, role));
				invitation.setRole(role);
				toSend.add(invitationRepository.save(invitation));
			}
		}
		return new Prepared(tenant, toSend, accepted);
	}

	@Transactional(readOnly = true)
	public Prepared prepareResend(Long tenantId, Long invitationId) {
		Tenant tenant = requireTenant(tenantId);
		TenantInvitation invitation = requireInvitation(tenantId, invitationId);
		if (invitation.isAccepted()) {
			throw new TenantInvitationConflictException(invitation.getEmail() + " has already joined the workspace.");
		}
		return new Prepared(tenant, List.of(invitation), List.of());
	}

	@Transactional
	public TenantInvitation recordSend(Long invitationId, String error) {
		TenantInvitation invitation = invitationRepository.findById(invitationId)
				.orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
		invitation.setSendCount(invitation.getSendCount() == null ? 1 : invitation.getSendCount() + 1);
		invitation.setSendError(error);
		if (error == null) {
			invitation.setSentAt(Instant.now());
		}
		return invitationRepository.save(invitation);
	}

	@Transactional(readOnly = true)
	public List<TenantInvitation> list(Long tenantId) {
		requireTenant(tenantId);
		return invitationRepository.findByTenantId(tenantId);
	}

	@Transactional
	public TenantInvitation revoke(Long tenantId, Long invitationId) {
		TenantInvitation invitation = requireInvitation(tenantId, invitationId);
		if (invitation.isAccepted()) {
			throw new TenantInvitationConflictException(
					invitation.getEmail() + " has already joined the workspace, so the invitation can't be revoked.");
		}
		invitationRepository.delete(invitation);
		return invitation;
	}

	private Tenant requireTenant(Long tenantId) {
		return tenantRepository.findById(tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + tenantId));
	}

	private TenantInvitation requireInvitation(Long tenantId, Long invitationId) {
		return invitationRepository.findById(invitationId).filter(i -> i.getTenantId().equals(tenantId))
				.orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
	}

	private static TenantInvitation newInvitation(Long tenantId, String email, Role role) {
		return TenantInvitation.builder().tenantId(tenantId).email(email).role(role)
				.publicToken(PublicTokens.newToken()).build();
	}
}
