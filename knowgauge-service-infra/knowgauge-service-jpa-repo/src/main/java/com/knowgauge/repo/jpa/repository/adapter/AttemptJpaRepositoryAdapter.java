package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Attempt;
import com.knowgauge.core.port.repository.AttemptRepository;
import com.knowgauge.repo.jpa.mapper.AttemptEntityMapper;
import com.knowgauge.repo.jpa.repository.AttemptJpaRepository;

@Repository
@Transactional
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
    @Transactional(readOnly = true)
    public Optional<Attempt> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Attempt> findByTestId(Long testId, Pageable pageable) {
        return jpaRepository.findByTestId(testId, pageable)
                .map(mapper::toDomain);
    }
}
