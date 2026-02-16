package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.infra.repository.jpa.mapper.TestEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TestJpaRepository;

@Repository
public class TestJpaRepositoryAdapter implements TestRepository {

	private final TestJpaRepository jpaRepository;
	private final TestEntityMapper mapper;

	public TestJpaRepositoryAdapter(TestJpaRepository jpaRepository, TestEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Test save(Test domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<Test> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}
}
