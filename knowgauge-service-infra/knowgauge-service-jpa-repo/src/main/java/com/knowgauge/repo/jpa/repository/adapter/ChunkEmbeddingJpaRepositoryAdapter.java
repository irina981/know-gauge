package com.knowgauge.repo.jpa.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.port.repository.ChunkEmbeddingRepository;
import com.knowgauge.repo.jpa.mapper.ChunkEmbeddingEntityMapper;
import com.knowgauge.repo.jpa.repository.ChunkEmbeddingJpaRepository;

@Repository
@Transactional
public class ChunkEmbeddingJpaRepositoryAdapter implements ChunkEmbeddingRepository {

	private final ChunkEmbeddingJpaRepository jpaRepository;
	private final ChunkEmbeddingEntityMapper mapper;

	public ChunkEmbeddingJpaRepositoryAdapter(ChunkEmbeddingJpaRepository jpaRepository,
			ChunkEmbeddingEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public ChunkEmbedding save(ChunkEmbedding domain) {
		return mapper.toDomain(jpaRepository.save(mapper.toEntity(domain)));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ChunkEmbedding> findByTenantIdAndChunkIdAndEmbeddingModel(Long tenantId, Long chunkId,
			String embeddingModel) {
		return jpaRepository.findByTenantIdAndChunkIdAndEmbeddingModel(tenantId, chunkId, embeddingModel)
				.map(mapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChunkEmbedding> findByTenantIdAndChunkIdInAndEmbeddingModel(Long tenantId, Collection<Long> chunkIds,
			String embeddingModel) {
		return jpaRepository.findByTenantIdAndChunkIdInAndEmbeddingModel(tenantId, chunkIds, embeddingModel).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	public void deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(Long tenantId, Long documentId,
			Integer documentVersion, String embeddingModel) {
		jpaRepository.deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(tenantId, documentId,
				documentVersion, embeddingModel);
	}

	@Override
	public void deleteByTenantIdAndChunkId(Long tenantId, Long chunkId) {
		jpaRepository.deleteByTenantIdAndChunkId(tenantId, chunkId);
	}
}
