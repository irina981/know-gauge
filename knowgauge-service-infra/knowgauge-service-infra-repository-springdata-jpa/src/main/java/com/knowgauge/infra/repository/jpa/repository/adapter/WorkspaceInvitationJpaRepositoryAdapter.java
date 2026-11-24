package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.WorkspaceInvitation;
import com.knowgauge.core.port.repository.WorkspaceInvitationRepository;
import com.knowgauge.infra.repository.jpa.mapper.WorkspaceInvitationEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.WorkspaceInvitationJpaRepository;

@Repository
public class WorkspaceInvitationJpaRepositoryAdapter implements WorkspaceInvitationRepository {

	private final WorkspaceInvitationJpaRepository jpaRepository;
	private final WorkspaceInvitationEntityMapper mapper;

	public WorkspaceInvitationJpaRepositoryAdapter(WorkspaceInvitationJpaRepository jpaRepository,
			WorkspaceInvitationEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public WorkspaceInvitation save(WorkspaceInvitation invitation) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(invitation)));
	}

	@Override
	public Optional<WorkspaceInvitation> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<WorkspaceInvitation> findByPublicToken(String publicToken) {
		return jpaRepository.findByPublicToken(publicToken).map(mapper::toDomain);
	}

	@Override
	public Optional<WorkspaceInvitation> findByTenantIdAndEmail(Long tenantId, String email) {
		return jpaRepository.findByTenantIdAndEmail(tenantId, email).map(mapper::toDomain);
	}

	@Override
	public List<WorkspaceInvitation> findByTenantId(Long tenantId) {
		return jpaRepository.findByTenantIdOrderByCreatedAtAscIdAsc(tenantId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<WorkspaceInvitation> findOldestPendingByEmail(String email) {
		return jpaRepository.findFirstByEmailAndAcceptedAtIsNullOrderByCreatedAtAscIdAsc(email).map(mapper::toDomain);
	}

	@Override
	public void delete(WorkspaceInvitation invitation) {
		jpaRepository.deleteById(invitation.getId());
	}
}
