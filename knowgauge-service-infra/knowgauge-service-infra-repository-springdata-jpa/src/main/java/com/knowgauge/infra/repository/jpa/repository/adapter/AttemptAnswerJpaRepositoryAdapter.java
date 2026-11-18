package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.core.port.repository.AttemptAnswerRepository;
import com.knowgauge.infra.repository.jpa.mapper.AttemptAnswerEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.AttemptAnswerJpaRepository;

@Repository
public class AttemptAnswerJpaRepositoryAdapter implements AttemptAnswerRepository {

	private final AttemptAnswerJpaRepository jpaRepository;
	private final AttemptAnswerEntityMapper mapper;

	public AttemptAnswerJpaRepositoryAdapter(AttemptAnswerJpaRepository jpaRepository,
			AttemptAnswerEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public AttemptAnswer save(AttemptAnswer domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public List<AttemptAnswer> saveAll(List<AttemptAnswer> answers) {
		return jpaRepository.saveAll(answers.stream().map(mapper::toEntity).toList()).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	public List<AttemptAnswer> findByTenantIdAndAttemptId(Long tenantId, Long attemptId) {
		return jpaRepository.findByTenantIdAndAttemptId(tenantId, attemptId).stream().map(mapper::toDomain)
				.toList();
	}
}
