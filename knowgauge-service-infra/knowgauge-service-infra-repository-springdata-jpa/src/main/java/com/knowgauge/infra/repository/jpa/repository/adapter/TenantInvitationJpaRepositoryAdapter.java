package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.TenantInvitation;
import com.knowgauge.core.port.repository.TenantInvitationRepository;
import com.knowgauge.infra.repository.jpa.mapper.TenantInvitationEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TenantInvitationJpaRepository;

@Repository
public class TenantInvitationJpaRepositoryAdapter implements TenantInvitationRepository {

	private final TenantInvitationJpaRepository jpaRepository;
	private final TenantInvitationEntityMapper mapper;

	public TenantInvitationJpaRepositoryAdapter(TenantInvitationJpaRepository jpaRepository,
			TenantInvitationEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public TenantInvitation save(TenantInvitation invitation) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(invitation)));
	}

	@Override
	public Optional<TenantInvitation> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<TenantInvitation> findByPublicToken(String publicToken) {
		return jpaRepository.findByPublicToken(publicToken).map(mapper::toDomain);
	}

	@Override
	public Optional<TenantInvitation> findByTenantIdAndEmail(Long tenantId, String email) {
		return jpaRepository.findByTenantIdAndEmail(tenantId, email).map(mapper::toDomain);
	}

	@Override
	public List<TenantInvitation> findByTenantId(Long tenantId) {
		return jpaRepository.findByTenantIdOrderByCreatedAtAscIdAsc(tenantId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<TenantInvitation> findOldestPendingByEmail(String email) {
		return jpaRepository.findFirstByEmailAndAcceptedAtIsNullOrderByCreatedAtAscIdAsc(email).map(mapper::toDomain);
	}

	@Override
	public void delete(TenantInvitation invitation) {
		jpaRepository.deleteById(invitation.getId());
	}
}
