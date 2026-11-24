package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TenantInvitationEntity;

@Repository
public interface TenantInvitationJpaRepository extends JpaRepository<TenantInvitationEntity, Long> {

	Optional<TenantInvitationEntity> findByPublicToken(String publicToken);

	Optional<TenantInvitationEntity> findByTenantIdAndEmail(Long tenantId, String email);

	List<TenantInvitationEntity> findByTenantIdOrderByCreatedAtAscIdAsc(Long tenantId);

	Optional<TenantInvitationEntity> findFirstByEmailAndAcceptedAtIsNullOrderByCreatedAtAscIdAsc(String email);
}
