package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.DocumentSection;
import com.knowgauge.core.port.repository.DocumentSectionRepository;
import com.knowgauge.repo.jpa.mapper.DocumentSectionEntityMapper;
import com.knowgauge.repo.jpa.repository.DocumentSectionJpaRepository;

@Repository
public class DocumentSectionJpaRepositoryAdapter implements DocumentSectionRepository {

    private final DocumentSectionJpaRepository jpaRepository;
    private final DocumentSectionEntityMapper mapper;

    public DocumentSectionJpaRepositoryAdapter(DocumentSectionJpaRepository jpaRepository, DocumentSectionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DocumentSection save(DocumentSection domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    public Optional<DocumentSection> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<DocumentSection> findByDocumentId(Long documentId, Pageable pageable) {
        return jpaRepository.findByDocumentId(documentId, pageable)
                .map(mapper::toDomain);
    }
}
