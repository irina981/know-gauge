package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.TestEvent;
import com.knowgauge.core.port.repository.TestEventRepository;
import com.knowgauge.infra.repository.jpa.mapper.TestEventEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TestEventJpaRepository;

@Repository
public class TestEventJpaRepositoryAdapter implements TestEventRepository {

	private final TestEventJpaRepository jpaRepository;
	private final TestEventEntityMapper mapper;

	public TestEventJpaRepositoryAdapter(TestEventJpaRepository jpaRepository, TestEventEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public TestEvent append(TestEvent event) {
		if (event.getId() != null) {
			throw new IllegalArgumentException("Events are append-only");
		}
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(event)));
	}

	@Override
	@Transactional
	public List<TestEvent> appendAll(List<TestEvent> events) {
		if (events.stream().anyMatch(e -> e.getId() != null)) {
			throw new IllegalArgumentException("Events are append-only");
		}
		return jpaRepository.saveAll(events.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TestEvent> findByTenantIdAndTestId(Long tenantId, Long testId) {
		return jpaRepository.findByTenantIdAndTestIdOrderByCreatedAtAscIdAsc(tenantId, testId).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TestEvent> findByTenantIdAndTestIdAndQuestionId(Long tenantId, Long testId, Long questionId) {
		return jpaRepository.findByTenantIdAndTestIdAndQuestionIdOrderByCreatedAtAscIdAsc(tenantId, testId, questionId)
				.stream().map(mapper::toDomain).toList();
	}
}
