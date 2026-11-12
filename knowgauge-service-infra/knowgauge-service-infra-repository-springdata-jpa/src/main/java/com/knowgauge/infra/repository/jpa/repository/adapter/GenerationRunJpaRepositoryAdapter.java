package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.port.repository.GenerationRunRepository;
import com.knowgauge.infra.repository.jpa.mapper.GenerationRunEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.GenerationRunJpaRepository;

@Repository
public class GenerationRunJpaRepositoryAdapter implements GenerationRunRepository {

	private final GenerationRunJpaRepository jpaRepository;
	private final GenerationRunEntityMapper mapper;

	public GenerationRunJpaRepositoryAdapter(GenerationRunJpaRepository jpaRepository,
			GenerationRunEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public GenerationRun save(GenerationRun domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<GenerationRun> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public List<GenerationRun> findByTenantIdAndTestId(Long tenantId, Long testId) {
		return jpaRepository.findByTenantIdAndTestIdOrderByIdAsc(tenantId, testId).stream().map(mapper::toDomain)
				.toList();
	}
}
