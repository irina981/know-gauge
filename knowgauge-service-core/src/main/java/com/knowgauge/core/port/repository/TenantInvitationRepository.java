package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.TenantInvitation;

public interface TenantInvitationRepository {
	TenantInvitation save(TenantInvitation invitation);

	Optional<TenantInvitation> findById(Long id);

	Optional<TenantInvitation> findByPublicToken(String publicToken);

	/** @param email lower-cased */
	Optional<TenantInvitation> findByTenantIdAndEmail(Long tenantId, String email);

	/** @return the workspace's invitations, oldest first */
	List<TenantInvitation> findByTenantId(Long tenantId);

	/** @return the oldest invitation to {@code email} (lower-cased) not accepted yet */
	Optional<TenantInvitation> findOldestPendingByEmail(String email);

	void delete(TenantInvitation invitation);
}
