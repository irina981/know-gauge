package com.knowgauge.infra.repository.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TestInvitationEntity;

@Repository
public interface TestInvitationJpaRepository extends JpaRepository<TestInvitationEntity, Long> {

	Optional<TestInvitationEntity> findByTenantIdAndId(Long tenantId, Long id);

	Optional<TestInvitationEntity> findByTenantIdAndPublicToken(Long tenantId, String publicToken);

	Optional<TestInvitationEntity> findByTenantIdAndTestIdAndEmail(Long tenantId, Long testId, String email);

	List<TestInvitationEntity> findByTenantIdAndTestIdOrderByCreatedAtAscIdAsc(Long tenantId, Long testId);
}
