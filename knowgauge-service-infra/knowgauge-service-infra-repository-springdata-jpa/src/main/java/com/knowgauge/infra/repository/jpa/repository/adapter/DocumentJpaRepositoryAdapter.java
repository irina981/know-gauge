package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Document;
import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.core.port.repository.DocumentRepository;
import com.knowgauge.infra.repository.jpa.mapper.DocumentEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.DocumentJpaRepository;

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
	public Optional<Document> findByTenantIdInAndId(Collection<Long> tenantIds, Long id) {
		return jpaRepository.findByTenantIdInAndId(tenantIds, id).map(mapper::toDomain);
	}

	@Override
	public Page<Document> findByTenantIdInAndTopicId(Collection<Long> tenantIds, Long topicId, Pageable pageable) {
		return jpaRepository.findByTenantIdInAndTopicId(tenantIds, topicId, pageable).map(mapper::toDomain);
	}

	@Override
	public void updateStorageKey(Long tenantId, Long id, String storageKey) {
		jpaRepository.updateStorageKey(tenantId, id, storageKey);
	}

	@Override
	public void deleteByTenantIdAndId(Long tenantId, Long id) {
		jpaRepository.deleteByTenantIdAndId(tenantId, id);
	}

	@Override
	public boolean existsByTenantIdAndTopicIdAndOriginalFileName(Long tenantId, Long topicId,
			String originalFileName) {
		return jpaRepository.existsByTenantIdAndTopicIdAndOriginalFileName(tenantId, topicId, originalFileName);
	}

	@Override
	public boolean existsByTenantIdAndTopicIdAndChecksum(Long tenantId, Long topicId, String contentHash) {
		return jpaRepository.existsByTenantIdAndTopicIdAndChecksum(tenantId, topicId, contentHash);
	}

	@Override
	public int markIngesting(Long tenantId, Long documentId) {
		return jpaRepository.markIngesting(tenantId, documentId);
	}

	@Override
	public int markIngested(Long tenantId, Long documentId) {
		return jpaRepository.markIngested(tenantId, documentId);
	}

	@Override
	public int markFailed(Long tenantId, Long documentId, String errorMessage) {
		return jpaRepository.markFailed(tenantId, documentId, errorMessage);
	}

	@Override
	public Map<Long, Long> countByTopicId(Collection<Long> tenantIds, DocumentStatus status) {
		Map<Long, Long> counts = new LinkedHashMap<>();
		for (Object[] row : jpaRepository.countByTopicId(tenantIds, status)) {
			counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
		}
		return counts;
	}

	@Override
	public List<Document> findByTenantIdInAndTopicIdIn(Collection<Long> tenantIds, List<Long> topicIds) {
		return jpaRepository.findByTenantIdInAndTopicIdIn(tenantIds, topicIds).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Long> findTenantIdsByStatus(DocumentStatus status) {
		return jpaRepository.findTenantIdsByStatus(status);
	}

	@Override
	public List<Document> findOldestByTenantIdAndStatus(Long tenantId, DocumentStatus status, int limit) {
		return jpaRepository
				.findByTenantIdAndStatusOrderByCreatedAtAscIdAsc(tenantId, status, PageRequest.of(0, limit))
				.stream().map(mapper::toDomain).toList();
	}
}
