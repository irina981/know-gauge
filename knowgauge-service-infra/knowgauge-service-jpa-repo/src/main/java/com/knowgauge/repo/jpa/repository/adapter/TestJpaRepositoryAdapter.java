package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.repo.jpa.mapper.TestMapper;
import com.knowgauge.repo.jpa.repository.TestJpaRepository;

@Repository
@Transactional
public class TestJpaRepositoryAdapter implements TestRepository {

    private final TestJpaRepository jpaRepository;
    private final TestMapper mapper;

    public TestJpaRepositoryAdapter(TestJpaRepository jpaRepository, TestMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Test save(Test domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Test> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Test> findByTopicId(Long topicId, Pageable pageable) {
        return jpaRepository.findByTopicId(topicId, pageable)
                .map(mapper::toDomain);
    }
}
