package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.infra.repository.jpa.mapper.AttemptEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.AttemptJpaRepository;

@Repository
public class AttemptJpaRepositoryAdapter implements AttemptRepository {

	private final AttemptJpaRepository jpaRepository;
	private final AttemptEntityMapper mapper;

	public AttemptJpaRepositoryAdapter(AttemptJpaRepository jpaRepository, AttemptEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Attempt save(Attempt domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<Attempt> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public Optional<Attempt> findByPublicToken(String publicToken) {
		return jpaRepository.findByPublicToken(publicToken).map(mapper::toDomain);
	}

	@Override
	public Optional<Attempt> findByTenantIdAndInvitationId(Long tenantId, Long invitationId) {
		return jpaRepository.findByTenantIdAndInvitationId(tenantId, invitationId).map(mapper::toDomain);
	}

	@Override
	public Optional<Attempt> findByTenantIdAndIdForUpdate(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndIdForUpdate(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public List<Attempt> findByTenantId(Long tenantId, Long testId, Long userId, String participantEmail) {
		return jpaRepository.findFiltered(tenantId, testId, userId, participantEmail).stream().map(mapper::toDomain).toList();
	}

	@Override
	public boolean existsByTenantIdAndTestId(Long tenantId, Long testId) {
		return jpaRepository.existsByTenantIdAndTestId(tenantId, testId);
	}
}
