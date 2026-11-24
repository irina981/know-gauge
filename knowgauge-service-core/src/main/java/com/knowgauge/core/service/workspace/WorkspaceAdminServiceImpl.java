package com.knowgauge.core.service.workspace;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.knowgauge.core.exception.MailDeliveryException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.notification.WorkspaceInvitationMailer;
import com.knowgauge.core.port.notification.WorkspaceInvitationMailer.WorkspaceInvitationMail;
import com.knowgauge.core.service.admin.TenantAdminService;
import com.knowgauge.core.service.content.ContentScope;
import com.knowgauge.core.service.invitation.InvitationProperties;
import com.knowgauge.core.service.workspace.WorkspaceAdminTransactionalServiceImpl.Prepared;

import lombok.extern.slf4j.Slf4j;

/**
 * Non-transactional: mail goes out between the short transactions of
 * {@link WorkspaceAdminTransactionalServiceImpl}, never inside one.
 */
@Slf4j
@Service
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {

	private static final int MAX_NAME_LENGTH = 200;

	private final WorkspaceAdminTransactionalServiceImpl transactionalService;
	private final WorkspaceInvitationMailer mailer;
	private final InvitationProperties properties;
	private final TenantAdminService tenantAdminService;
	private final ContentScope contentScope;

	public WorkspaceAdminServiceImpl(WorkspaceAdminTransactionalServiceImpl transactionalService,
			WorkspaceInvitationMailer mailer, InvitationProperties properties, TenantAdminService tenantAdminService,
			ContentScope contentScope) {
		this.transactionalService = transactionalService;
		this.mailer = mailer;
		this.properties = properties;
		this.tenantAdminService = tenantAdminService;
		this.contentScope = contentScope;
	}

	@Override
	public CreatedWorkspace create(TenantType type, String name, List<String> emails, String personalMessage) {
		if (type == null) {
			throw new IllegalArgumentException("Choose the type of the workspace.");
		}
		String workspaceName = name == null ? "" : name.trim().replaceAll("\\s+", " ");
		if (workspaceName.isEmpty()) {
			throw new IllegalArgumentException("Enter a name for the workspace.");
		}
		if (workspaceName.length() > MAX_NAME_LENGTH) {
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

		Prepared prepared = transactionalService.create(type, workspaceName, addresses);
		Long tenantId = prepared.tenant().getId();
		if (type == TenantType.LIBRARY) {
			// Everyone reads it from now on, and an admin may fill it.
			contentScope.refreshLibraries();
		}
		List<WorkspaceInvitation> invitations = sendAll(prepared, message);
		log.info("Tenant {} - created as {} \"{}\" with {} invitations", tenantId, type, workspaceName,
				invitations.size());
		return new CreatedWorkspace(tenantAdminService.getTenant(tenantId), invitations);
	}

	@Override
	public List<WorkspaceInvitation> invite(Long tenantId, List<String> emails, String personalMessage) {
		List<String> addresses = InvitationInput.emails(emails, properties.getMaxPerRequest());
		String message = InvitationInput.message(personalMessage);
		Prepared prepared = transactionalService.prepareInvite(tenantId, addresses);
		List<WorkspaceInvitation> result = new ArrayList<>(prepared.accepted());
		result.addAll(sendAll(prepared, message));
		return result;
	}

	@Override
	public List<WorkspaceInvitation> listInvitations(Long tenantId) {
		return transactionalService.list(tenantId);
	}

	@Override
	public WorkspaceInvitation resend(Long tenantId, Long invitationId, String personalMessage) {
		String message = InvitationInput.message(personalMessage);
		Prepared prepared = transactionalService.prepareResend(tenantId, invitationId);
		return send(prepared.tenant(), prepared.toSend().get(0), message);
	}

	@Override
	public void revoke(Long tenantId, Long invitationId) {
		WorkspaceInvitation revoked = transactionalService.revoke(tenantId, invitationId);
		log.info("Tenant {} - invitation {} to {} revoked", tenantId, invitationId, revoked.getEmail());
	}

	private List<WorkspaceInvitation> sendAll(Prepared prepared, String message) {
		List<WorkspaceInvitation> result = new ArrayList<>(prepared.toSend().size());
		int sent = 0;
		for (WorkspaceInvitation invitation : prepared.toSend()) {
			WorkspaceInvitation updated = send(prepared.tenant(), invitation, message);
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

	private WorkspaceInvitation send(Tenant tenant, WorkspaceInvitation invitation, String message) {
		String error = null;
		try {
			mailer.send(new WorkspaceInvitationMail(invitation.getEmail(), tenant.getName(), tenant.getType(),
					properties.workspaceLinkFor(invitation.getPublicToken()), message));
		} catch (MailDeliveryException e) {
			error = e.getMessage();
			log.warn("Tenant {} - invitation {} to {} could not be sent: {}", tenant.getId(), invitation.getId(),
					invitation.getEmail(), error);
		}
		return transactionalService.recordSend(invitation.getId(), error);
	}
}
