package com.knowgauge.repo.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.repo.jpa.mapper.TestQuestionEntityMapper;
import com.knowgauge.repo.jpa.repository.TestQuestionJpaRepository;

@Repository
public class TestQuestionJpaRepositoryAdapter implements TestQuestionRepository {

	private final TestQuestionJpaRepository jpaRepository;
	private final TestQuestionEntityMapper mapper;

	public TestQuestionJpaRepositoryAdapter(TestQuestionJpaRepository jpaRepository, TestQuestionEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public TestQuestion save(TestQuestion domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<TestQuestion> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<TestQuestion> findByTestId(Long testId) {
		return jpaRepository.findByTestId(testId).stream().map(mapper::toDomain).toList();
	}
}
