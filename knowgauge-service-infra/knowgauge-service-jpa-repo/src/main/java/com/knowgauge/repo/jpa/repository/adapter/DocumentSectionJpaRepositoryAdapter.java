package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.DocumentSection;
import com.knowgauge.core.port.repository.DocumentSectionRepository;
import com.knowgauge.repo.jpa.mapper.DocumentSectionEntityMapper;
import com.knowgauge.repo.jpa.repository.DocumentSectionJpaRepository;

@Repository
@Transactional
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
    @Transactional(readOnly = true)
    public Optional<DocumentSection> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentSection> findByDocumentId(Long documentId, Pageable pageable) {
        return jpaRepository.findByDocumentId(documentId, pageable)
                .map(mapper::toDomain);
    }

	private <U> U toDomain(DocumentSection documentsection1) {
		return null;
	}
}
