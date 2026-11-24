package com.knowgauge.core.service.tenant;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.notification.TenantInvitationMailer;
import com.knowgauge.core.port.notification.TenantInvitationMailer.TenantInvitationMail;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.core.service.content.ContentScope;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.tenant.TenantProvisioningTransactionalServiceImpl.Prepared;

import lombok.extern.slf4j.Slf4j;

/**
 * Non-transactional: mail goes out between the short transactions of
 * {@link TenantProvisioningTransactionalServiceImpl}, never inside one.
 */
@Slf4j
@Service
public class TenantProvisioningServiceImpl implements TenantProvisioningService {

	private static final int MAX_NAME_LENGTH = 200;

	/** The roles an invitation may give: roles in the workspace, never {@link Role#ADMIN}. */
	private static final Set<Role> TENANT_ROLES = EnumSet.of(Role.TENANT_ADMIN, Role.TENANT_USER, Role.TENANT_VIEWER);

	private final TenantProvisioningTransactionalServiceImpl transactionalService;
	private final TenantInvitationMailer mailer;
	private final InvitationProperties properties;
	private final TenantAdminService tenantAdminService;
	private final ContentScope contentScope;

	public TenantProvisioningServiceImpl(TenantProvisioningTransactionalServiceImpl transactionalService,
			TenantInvitationMailer mailer, InvitationProperties properties, TenantAdminService tenantAdminService,
			ContentScope contentScope) {
		this.transactionalService = transactionalService;
		this.mailer = mailer;
		this.properties = properties;
		this.tenantAdminService = tenantAdminService;
		this.contentScope = contentScope;
	}

	@Override
	public CreatedTenant create(TenantType type, String name, List<String> emails, String personalMessage) {
		if (type == null) {
			throw new IllegalArgumentException("Choose the type of the workspace.");
		}
		String tenantName = name == null ? "" : name.trim().replaceAll("\\s+", " ");
		if (tenantName.isEmpty()) {
			throw new IllegalArgumentException("Enter a name for the workspace.");
		}
		if (tenantName.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("The name can be at most " + MAX_NAME_LENGTH + " characters.");
		}
		List<String> addresses = InvitationInput.emails(emails, properties.getMaxPerRequest());
		String message = InvitationInput.message(personalMessage);
		switch (type) {
		case LIBRARY -> {
			if (!addresses.isEmpty()) {
				throw new IllegalArgumentException("A shared library has no members, so it takes no invitations.");
			}
		}
		case PERSONAL -> {
			if (addresses.size() != 1) {
				throw new IllegalArgumentException(
						"A personal workspace is for one person: invite exactly one email address.");
			}
		}
		case ORGANIZATION -> {
			if (addresses.isEmpty()) {
				throw new IllegalArgumentException("Invite at least one person to administer the organization.");
			}
		}
		}

		Prepared prepared = transactionalService.create(type, tenantName, addresses);
		Long tenantId = prepared.tenant().getId();
		if (type == TenantType.LIBRARY) {
			// Everyone reads it from now on, and an admin may fill it.
			contentScope.refreshLibraries();
		}
		List<TenantInvitation> invitations = sendAll(prepared, message);
		log.info("Tenant {} - created as {} \"{}\" with {} invitations", tenantId, type, tenantName,
				invitations.size());
		return new CreatedTenant(tenantAdminService.getTenant(tenantId), invitations);
	}

	@Override
	public List<TenantInvitation> invite(Long tenantId, List<String> emails, Role role, String personalMessage) {
		if (role == null || !TENANT_ROLES.contains(role)) {
			throw new IllegalArgumentException("Invite people as TENANT_ADMIN, TENANT_USER or TENANT_VIEWER.");
		}
		List<String> addresses = InvitationInput.emails(emails, properties.getMaxPerRequest());
		String message = InvitationInput.message(personalMessage);
		Prepared prepared = transactionalService.prepareInvite(tenantId, addresses, role);
		List<TenantInvitation> result = new ArrayList<>(prepared.accepted());
		result.addAll(sendAll(prepared, message));
		return result;
	}

	@Override
	public List<TenantInvitation> listInvitations(Long tenantId) {
		return transactionalService.list(tenantId);
	}

	@Override
	public TenantInvitation resend(Long tenantId, Long invitationId, String personalMessage) {
		String message = InvitationInput.message(personalMessage);
		Prepared prepared = transactionalService.prepareResend(tenantId, invitationId);
		return send(prepared.tenant(), prepared.toSend().get(0), message);
	}

	@Override
	public void revoke(Long tenantId, Long invitationId) {
		TenantInvitation revoked = transactionalService.revoke(tenantId, invitationId);
		log.info("Tenant {} - invitation {} to {} revoked", tenantId, invitationId, revoked.getEmail());
	}

	private List<TenantInvitation> sendAll(Prepared prepared, String message) {
		List<TenantInvitation> result = new ArrayList<>(prepared.toSend().size());
		int sent = 0;
		for (TenantInvitation invitation : prepared.toSend()) {
			TenantInvitation updated = send(prepared.tenant(), invitation, message);
			if (updated.getSendError() == null) {
				sent++;
			}
			result.add(updated);
		}
		if (!prepared.toSend().isEmpty()) {
			log.info("Tenant {} - {} invitations, {} emails sent", prepared.tenant().getId(),
					prepared.toSend().size(), sent);
		}
		return result;
	}

	private TenantInvitation send(Tenant tenant, TenantInvitation invitation, String message) {
		String error = null;
		try {
			mailer.send(new TenantInvitationMail(invitation.getEmail(), tenant.getName(), tenant.getType(),
					properties.tenantLinkFor(invitation.getPublicToken()), message));
		} catch (MailDeliveryException e) {
			error = e.getMessage();
			log.warn("Tenant {} - invitation {} to {} could not be sent: {}", tenant.getId(), invitation.getId(),
					invitation.getEmail(), error);
		}
		return transactionalService.recordSend(invitation.getId(), error);
	}
}
