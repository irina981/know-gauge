package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.repo.jpa.mapper.DocumentMapper;
import com.knowgauge.repo.jpa.repository.DocumentJpaRepository;

@Repository
@Transactional
public class DocumentJpaRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;
    private final DocumentMapper mapper;

    public DocumentJpaRepositoryAdapter(DocumentJpaRepository jpaRepository, DocumentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Document> findByTopicId(Long topicId, Pageable pageable) {
        return jpaRepository.findByTopicId(topicId, pageable)
                .map(mapper::toDomain);
    }
}
