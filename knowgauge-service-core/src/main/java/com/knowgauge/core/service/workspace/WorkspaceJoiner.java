package com.knowgauge.core.service.workspace;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.knowgauge.core.exception.WorkspaceInvitationConflictException;
import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.User;
import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.model.enums.Role;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.core.port.repository.UserRepository;
import com.knowgauge.core.port.repository.WorkspaceInvitationRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Puts a user into the workspace of an invitation (ADR015), inside the caller's transaction: the user's one tenant
 * becomes the invited one ({@code users.tenant_id}), their tenant roles are replaced by the invited role (an
 * {@link Role#ADMIN} stays one), a personal workspace without an owner gets them as its owner, and the invitation is
 * marked accepted. A workspace the user had before keeps its content; it just has no users any more.
 */
@Slf4j
@Component
public class WorkspaceJoiner {

	private static final Set<Role> TENANT_ROLES = EnumSet.of(Role.TENANT_ADMIN, Role.TENANT_USER, Role.TENANT_VIEWER);

	private final UserRepository userRepository;
	private final TenantRepository tenantRepository;
	private final WorkspaceInvitationRepository invitationRepository;

	public WorkspaceJoiner(UserRepository userRepository, TenantRepository tenantRepository,
			WorkspaceInvitationRepository invitationRepository) {
		this.userRepository = userRepository;
		this.tenantRepository = tenantRepository;
		this.invitationRepository = invitationRepository;
	}

	/**
	 * @param user a stored user, or a new one (no id) that is saved here
	 * @return the saved user, now in the invited workspace
	 * @throws WorkspaceInvitationConflictException when the workspace is a library or a personal one with another owner
	 */
	public User join(User user, WorkspaceInvitation invitation, Tenant tenant, Instant now) {
		if (tenant.getType() == TenantType.LIBRARY) {
			throw new WorkspaceInvitationConflictException("A shared library has no members.");
		}
		boolean personal = tenant.getType() == TenantType.PERSONAL;
		if (personal && tenant.getOwnerUserId() != null && !tenant.getOwnerUserId().equals(user.getId())) {
			throw new WorkspaceInvitationConflictException(
					"The personal workspace " + tenant.getName() + " already belongs to someone else.");
		}
		Long previousTenantId = user.getTenantId();
		Set<Role> roles = EnumSet.noneOf(Role.class);
		if (user.getRoles() != null) {
			roles.addAll(user.getRoles());
		}
		roles.removeAll(TENANT_ROLES);
		roles.add(invitation.getRole());
		user.setRoles(roles);
		user.setTenantId(tenant.getId());
		User saved = userRepository.save(user);

		if (personal && tenant.getOwnerUserId() == null) {
			tenant.setOwnerUserId(saved.getId());
			tenantRepository.save(tenant);
		}
		invitation.setAcceptedAt(now);
		invitation.setAcceptedUserId(saved.getId());
		invitationRepository.save(invitation);
		log.info("User {} joined tenant {} as {} through invitation {} (tenant before: {})", saved.getId(),
				tenant.getId(), invitation.getRole(), invitation.getId(), previousTenantId);
		return saved;
	}
}
