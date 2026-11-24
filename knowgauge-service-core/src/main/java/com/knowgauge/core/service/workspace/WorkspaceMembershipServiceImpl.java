package com.knowgauge.core.service.workspace;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.exception.WorkspaceInvitationConflictException;
import com.knowgauge.core.exception.WorkspaceInvitationForbiddenException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.core.port.repository.WorkspaceInvitationRepository;
import com.knowgauge.core.util.Emails;

@Service
public class WorkspaceMembershipServiceImpl implements WorkspaceMembershipService {

	private static final String INVALID_LINK = "This invitation link is not valid. It may have been revoked.";

	private final WorkspaceInvitationRepository invitationRepository;
	private final TenantRepository tenantRepository;
	private final UserRepository userRepository;
	private final WorkspaceJoiner joiner;
	private final ExecutionContext executionContext;

	public WorkspaceMembershipServiceImpl(WorkspaceInvitationRepository invitationRepository,
			TenantRepository tenantRepository, UserRepository userRepository, WorkspaceJoiner joiner,
			ExecutionContext executionContext) {
		this.invitationRepository = invitationRepository;
		this.tenantRepository = tenantRepository;
		this.userRepository = userRepository;
		this.joiner = joiner;
		this.executionContext = executionContext;
	}

	@Override
	@Transactional(readOnly = true)
	public InvitationPreview preview(String token) {
		WorkspaceInvitation invitation = requireInvitation(token);
		Tenant tenant = requireTenant(invitation);
		return new InvitationPreview(tenant.getName(), tenant.getType(), invitation.getEmail(), invitation.isAccepted());
	}

	@Override
	@Transactional
	public Joined accept(String token, boolean emailVerified) {
		WorkspaceInvitation invitation = requireInvitation(token);
		OptionalLong userId = currentUserId();
		User user = (userId.isPresent() ? userRepository.findById(userId.getAsLong()) : Optional.<User>empty())
				.orElseThrow(() -> new WorkspaceInvitationForbiddenException("Sign in to accept the invitation."));
		Tenant tenant = requireTenant(invitation);

		if (invitation.isAccepted()) {
			if (Objects.equals(invitation.getAcceptedUserId(), user.getId())) {
				return new Joined(tenant.getId(), tenant.getName(), null);
			}
			throw new WorkspaceInvitationConflictException("This invitation has already been accepted.");
		}
		if (!emailVerified) {
			throw new WorkspaceInvitationForbiddenException(
					"Verify your email address first, then open the invitation link again.");
		}
		String email = Emails.normalizeOrNull(user.getEmail());
		if (!invitation.getEmail().equals(email)) {
			throw new WorkspaceInvitationForbiddenException("This invitation was sent to " + invitation.getEmail()
					+ ", but you are signed in as " + (user.getEmail() == null ? "a user without an email" : user.getEmail())
					+ ". Sign in with the invited address to accept it.");
		}
		Long previousTenantId = user.getTenantId();
		joiner.join(user, invitation, tenant, Instant.now());
		return new Joined(tenant.getId(), tenant.getName(),
				Objects.equals(previousTenantId, tenant.getId()) ? null : previousTenantId);
	}

	private OptionalLong currentUserId() {
		OptionalLong userId = executionContext.userId();
		// The system user (anonymous requests in some modes) is nobody here.
		return userId.isPresent() && userId.getAsLong() == ExecutionContext.SYSTEM_USER_ID ? OptionalLong.empty() : userId;
	}

	private WorkspaceInvitation requireInvitation(String token) {
		if (token == null || token.isBlank()) {
			throw new ResourceNotFoundException(INVALID_LINK);
		}
		return invitationRepository.findByPublicToken(token.trim())
				.orElseThrow(() -> new ResourceNotFoundException(INVALID_LINK));
	}

	private Tenant requireTenant(WorkspaceInvitation invitation) {
		return tenantRepository.findById(invitation.getTenantId())
				.orElseThrow(() -> new ResourceNotFoundException(INVALID_LINK));
	}
}
