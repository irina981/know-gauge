package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.WorkspaceInvitation;

public interface WorkspaceInvitationRepository {
	WorkspaceInvitation save(WorkspaceInvitation invitation);

	Optional<WorkspaceInvitation> findById(Long id);

	Optional<WorkspaceInvitation> findByPublicToken(String publicToken);

	/** @param email lower-cased */
	Optional<WorkspaceInvitation> findByTenantIdAndEmail(Long tenantId, String email);

	/** @return the workspace's invitations, oldest first */
	List<WorkspaceInvitation> findByTenantId(Long tenantId);

	/** @return the oldest invitation to {@code email} (lower-cased) not accepted yet */
	Optional<WorkspaceInvitation> findOldestPendingByEmail(String email);

	void delete(WorkspaceInvitation invitation);
}
