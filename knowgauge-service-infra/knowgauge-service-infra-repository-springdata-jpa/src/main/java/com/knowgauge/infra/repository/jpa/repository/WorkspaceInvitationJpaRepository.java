package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.WorkspaceInvitationEntity;

@Repository
public interface WorkspaceInvitationJpaRepository extends JpaRepository<WorkspaceInvitationEntity, Long> {

	Optional<WorkspaceInvitationEntity> findByPublicToken(String publicToken);

	Optional<WorkspaceInvitationEntity> findByTenantIdAndEmail(Long tenantId, String email);

	List<WorkspaceInvitationEntity> findByTenantIdOrderByCreatedAtAscIdAsc(Long tenantId);

	Optional<WorkspaceInvitationEntity> findFirstByEmailAndAcceptedAtIsNullOrderByCreatedAtAscIdAsc(String email);
}
