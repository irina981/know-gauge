package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Test;
import com.knowgauge.core.model.enums.TestStatus;
import com.knowgauge.core.port.repository.TestRepository;
import com.knowgauge.infra.repository.jpa.entity.DocumentChunkEntity;
import com.knowgauge.infra.repository.jpa.entity.TestEntity;
import com.knowgauge.infra.repository.jpa.mapper.TestEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.DocumentChunkJpaRepository;
import com.knowgauge.infra.repository.jpa.repository.TestJpaRepository;

@Repository
public class TestJpaRepositoryAdapter implements TestRepository {

	private final TestJpaRepository jpaRepository;
	private final DocumentChunkJpaRepository documentChunkJpaRepository;
	private final TestEntityMapper mapper;

	public TestJpaRepositoryAdapter(TestJpaRepository jpaRepository, TestEntityMapper mapper, DocumentChunkJpaRepository documentChunkJpaRepository) {
		this.jpaRepository = jpaRepository;
		this.documentChunkJpaRepository = documentChunkJpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Test save(Test domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	public Optional<Test> findByTenantIdAndId(Long tenantId, Long id) {
		return jpaRepository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
	}

	@Override
	public List<Test> findByTenantId(Long tenantId) {
		return jpaRepository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Test> findByTenantIdAndStatus(Long tenantId, TestStatus status) {
		return jpaRepository.findByTenantIdAndStatus(tenantId, status).stream().map(mapper::toDomain).toList();
	}

	@Override
	public void deleteByTenantIdAndId(Long tenantId, Long id) {
		jpaRepository.deleteByTenantIdAndId(tenantId, id);
	}
	
	@Override
	public void setUsedChunks(Long tenantId, Long testId, List<Long> chunkIds) {

	    TestEntity test = jpaRepository.findByTenantIdAndId(tenantId, testId)
	            .orElseThrow(() -> new IllegalArgumentException("Test not found: " + testId));

	    List<DocumentChunkEntity> chunkEntities =
	    		documentChunkJpaRepository.findByTenantIdAndIdIn(tenantId, chunkIds);

	    // IMPORTANT: mutate in place (Hibernate PersistentBag stays mutable)
	    test.getUsedChunks().clear();
	    test.getUsedChunks().addAll(chunkEntities);

	    // no save() necessary if test is managed in the same persistence context
	}

	@Override
	public int markGenerated(Long tenantId, Long testId, Instant finishedAt) {
		return jpaRepository.markGenerated(tenantId, testId, finishedAt);
	}

	@Override
	public int markFailed(Long tenantId, Long testId, String errorMessage, Instant failedAt) {
		return jpaRepository.markFailed(tenantId, testId, errorMessage, failedAt);
	}
}
