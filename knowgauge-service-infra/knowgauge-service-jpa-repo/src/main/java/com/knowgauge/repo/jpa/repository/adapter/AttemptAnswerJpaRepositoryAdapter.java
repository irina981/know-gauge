package com.knowgauge.repo.jpa.repository.adapter;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.AttemptAnswer;
import com.knowgauge.core.port.repository.AttemptAnswerRepository;
import com.knowgauge.repo.jpa.mapper.AttemptAnswerEntityMapper;
import com.knowgauge.repo.jpa.repository.AttemptAnswerJpaRepository;

@Repository
@Transactional
public class AttemptAnswerJpaRepositoryAdapter implements AttemptAnswerRepository {

    private final AttemptAnswerJpaRepository jpaRepository;
    private final AttemptAnswerEntityMapper mapper;

    public AttemptAnswerJpaRepositoryAdapter(
    		AttemptAnswerJpaRepository jpaRepository,
            AttemptAnswerEntityMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AttemptAnswer save(AttemptAnswer domain) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(domain))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttemptAnswer> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttemptAnswer> findByAttemptId(Long attemptId, Pageable pageable) {
        return jpaRepository.findByAttemptId(attemptId, pageable)
                .map(mapper::toDomain);
    }
}
