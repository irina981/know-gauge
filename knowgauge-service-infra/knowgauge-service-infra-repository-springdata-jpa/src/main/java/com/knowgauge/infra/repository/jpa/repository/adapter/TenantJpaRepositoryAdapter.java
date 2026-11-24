package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Tenant;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.enums.TenantType;
import com.knowgauge.core.port.repository.TenantRepository;
import com.knowgauge.infra.repository.jpa.mapper.TenantEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TenantJpaRepository;

@Repository
public class TenantJpaRepositoryAdapter implements TenantRepository {

	private final TenantJpaRepository jpaRepository;
	private final TenantEntityMapper mapper;

	public TenantJpaRepositoryAdapter(TenantJpaRepository jpaRepository, TenantEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Tenant save(Tenant tenant) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(tenant)));
	}

	@Override
	public Optional<Tenant> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Long> findIdsByType(TenantType type) {
		return jpaRepository.findIdsByType(type);
	}

	@Override
	public List<TenantSummary> findTenantSummaries() {
		return jpaRepository.findTenantSummaries();
	}
}
