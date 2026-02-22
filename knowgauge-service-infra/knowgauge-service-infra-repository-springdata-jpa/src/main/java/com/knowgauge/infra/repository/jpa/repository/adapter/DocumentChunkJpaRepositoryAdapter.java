package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.DocumentChunk;
import com.knowgauge.core.port.repository.DocumentChunkRepository;
import com.knowgauge.infra.repository.jpa.mapper.DocumentChunkEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.DocumentChunkJpaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class DocumentChunkJpaRepositoryAdapter implements DocumentChunkRepository {
	@PersistenceContext
	private EntityManager em;

	private final DocumentChunkJpaRepository jpaRepository;
	private final DocumentChunkEntityMapper mapper;

	public DocumentChunkJpaRepositoryAdapter(DocumentChunkJpaRepository jpaRepository,
			DocumentChunkEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public DocumentChunk save(DocumentChunk domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}
	
	@Override
	public List<DocumentChunk> saveAll(List<DocumentChunk> chunks) {
		return jpaRepository.saveAll(chunks.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DocumentChunk> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findById(id).filter(e -> tenantId.equals(e.getTenantId())).map(mapper::toDomain);
		// NOTE: If you want it truly "just a repo call",
		// add findByTenantIdAndId(...) to JpaRepository and call it directly.
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentChunk> findByTenantIdAndDocumentIdAndDocumentVersionOrderByOrdinal(Long tenantId,
			Long documentId, Integer documentVersion) {
		return jpaRepository
				.findByTenantIdAndDocumentIdAndDocumentVersionOrderByOrdinal(tenantId, documentId, documentVersion)
				.stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DocumentChunk> findByTenantIdAndDocumentIdAndDocumentVersion(Long tenantId, Long documentId,
			Integer documentVersion, Pageable pageable) {
		return jpaRepository
				.findByTenantIdAndDocumentIdAndDocumentVersion(tenantId, documentId, documentVersion, pageable)
				.map(mapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DocumentChunk> findByTenantIdAndTopicId(Long tenantId, Long topicId, Pageable pageable) {
		return jpaRepository.findByTenantIdAndTopicId(tenantId, topicId, pageable).map(mapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentChunk> findByTenantIdAndTopicIdIn(Long tenantId, Collection<Long> topicIds) {
		return jpaRepository.findByTenantIdAndTopicIdIn(tenantId, topicIds).stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentChunk> findByTenantIdAndIdIn(Long tenantId, Collection<Long> chunkIds) {
		return jpaRepository.findByTenantIdAndIdIn(tenantId, chunkIds).stream().map(mapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DocumentChunk> findByTenantIdAndChecksum(Long tenantId, String checksum) {
		return jpaRepository.findByTenantIdAndChecksum(tenantId, checksum).map(mapper::toDomain);
	}

	@Override
	public void deleteByTenantIdAndDocumentIdAndDocumentVersion(Long tenantId, Long documentId,
			Integer documentVersion) {
		jpaRepository.deleteByTenantIdAndDocumentIdAndDocumentVersion(tenantId, documentId, documentVersion);
		em.flush();
		em.clear();
	}
}
