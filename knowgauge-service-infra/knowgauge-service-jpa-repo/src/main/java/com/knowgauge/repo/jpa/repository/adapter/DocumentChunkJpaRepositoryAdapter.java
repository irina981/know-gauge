package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.repo.jpa.mapper.DocumentChunkMapper;
import com.knowgauge.repo.jpa.repository.DocumentChunkJpaRepository;

@Repository
@Transactional
public class DocumentChunkJpaRepositoryAdapter implements DocumentChunkRepository {

    private final DocumentChunkJpaRepository jpaRepository;
    private final DocumentChunkMapper mapper;

    public DocumentChunkJpaRepositoryAdapter(DocumentChunkJpaRepository jpaRepository, DocumentChunkMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DocumentChunk save(DocumentChunk domain) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentChunk> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentChunk> findByDocumentId(Long documentId, Pageable pageable) {
        return jpaRepository.findByDocumentId(documentId, pageable)
                .map(mapper::toDomain);
    }
}
