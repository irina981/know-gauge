package com.knowgauge.repo.jpa.repository.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.TopicRepository;
import com.knowgauge.repo.jpa.mapper.TopicMapper;
import com.knowgauge.repo.jpa.repository.TopicJpaRepository;

@Repository
@Transactional
public class TopicJpaRepositoryAdapter implements TopicRepository {

    private final TopicJpaRepository jpaRepository;
    private final TopicMapper mapper;

    public TopicJpaRepositoryAdapter(TopicJpaRepository jpaRepository, TopicMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Topic save(Topic domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Topic> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> findByParentId(Long parentId) {
        return jpaRepository.findByParentId(parentId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
