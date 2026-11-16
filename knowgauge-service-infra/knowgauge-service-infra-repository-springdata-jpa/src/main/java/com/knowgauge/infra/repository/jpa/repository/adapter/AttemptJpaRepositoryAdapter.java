package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
	public Optional<Attempt> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Attempt> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public Optional<Attempt> findByTenantIdAndIdForUpdate(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndIdForUpdate(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public List<Attempt> findByTenantId(Long tenantId, Long testId, Long userId) {
		return jpaRepository.findFiltered(tenantId, testId, userId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public Page<Attempt> findByTestId(Long testId, Pageable pageable) {
		return jpaRepository.findByTestId(testId, pageable).map(mapper::toDomain);
	}

	@Override
	public boolean existsByTenantIdAndTestId(Long tenantId, Long testId) {
		return jpaRepository.existsByTenantIdAndTestId(tenantId, testId);
	}
}
