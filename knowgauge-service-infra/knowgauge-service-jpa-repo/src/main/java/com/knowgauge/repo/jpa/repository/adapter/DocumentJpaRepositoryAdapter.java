package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.repo.jpa.mapper.DocumentEntityMapper;
import com.knowgauge.repo.jpa.repository.DocumentJpaRepository;

@Repository
public class DocumentJpaRepositoryAdapter implements DocumentRepository {

	private final DocumentJpaRepository jpaRepository;
	private final DocumentEntityMapper mapper;

	public DocumentJpaRepositoryAdapter(DocumentJpaRepository jpaRepository, DocumentEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Document save(Document domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<Document> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public Page<Document> findByTopicId(Long topicId, Pageable pageable) {
		return jpaRepository.findByTopicId(topicId, pageable).map(mapper::toDomain);
	}

	public void updateStorageKey(Long id, String storageKey) {
		jpaRepository.updateStorageKey(id, storageKey);
	}

	@Override
	public void deleteById(Long id) {
		jpaRepository.deleteById(id);

	}

	@Override
	public boolean existsByTopicIdAndOriginalFileName(Long topicId, String originalFileName) {
		return jpaRepository.existsByTopicIdAndOriginalFileName(topicId, originalFileName);
	}

	@Override
	public boolean existsByTopicIdAndChecksum(Long topicId, String contentHash) {
		return jpaRepository.existsByTopicIdAndChecksum(topicId, contentHash);
	}

	@Override
	public int updateStatusIfCurrent(Long documentId, DocumentStatus fromStatus, DocumentStatus toStatus) {
		return jpaRepository.updateStatusIfCurrent(documentId, fromStatus, toStatus);
	}

	@Override
	public int markIngested(Long documentId) {
		return jpaRepository.markIngested(documentId);
	}

	@Override
	public int markFailed(Long documentId, String errorMessage) {
		return jpaRepository.markFailed(documentId, errorMessage);
	}
}
