package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.GenerationRun;
import com.knowgauge.core.port.repository.GenerationRunRepository;
import com.knowgauge.repo.jpa.mapper.GenerationRunMapper;
import com.knowgauge.repo.jpa.repository.GenerationRunJpaRepository;

@Repository
@Transactional
public class GenerationRunJpaRepositoryAdapter implements GenerationRunRepository {

    private final GenerationRunJpaRepository jpaRepository;
    private final GenerationRunMapper mapper;

    public GenerationRunJpaRepositoryAdapter(GenerationRunJpaRepository jpaRepository, GenerationRunMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GenerationRun save(GenerationRun domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GenerationRun> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GenerationRun> findByTestId(Long testId, Pageable pageable) {
        return jpaRepository.findByTestId(testId, pageable)
                .map(mapper::toDomain);
    }
}
