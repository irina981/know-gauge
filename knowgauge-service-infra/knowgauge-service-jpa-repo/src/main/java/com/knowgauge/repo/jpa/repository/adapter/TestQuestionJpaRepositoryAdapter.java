package com.knowgauge.repo.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.port.repository.TestQuestionRepository;
import com.knowgauge.repo.jpa.mapper.TestQuestionEntityMapper;
import com.knowgauge.repo.jpa.repository.TestQuestionJpaRepository;

@Repository
@Transactional
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
    @Transactional(readOnly = true)
    public Optional<TestQuestion> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestQuestion> findByTestId(Long testId) {
        return jpaRepository.findByTestId(testId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
