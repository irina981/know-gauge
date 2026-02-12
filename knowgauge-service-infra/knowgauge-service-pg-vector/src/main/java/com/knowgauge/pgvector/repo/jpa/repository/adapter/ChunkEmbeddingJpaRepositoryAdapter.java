package com.knowgauge.pgvector.repo.jpa.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.port.repository.ChunkEmbeddingRepository;
import com.knowgauge.pgvector.repo.jpa.mapper.ChunkEmbeddingEntityMapper;
import com.knowgauge.pgvector.repo.jpa.repository.ChunkEmbeddingJpaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class ChunkEmbeddingJpaRepositoryAdapter implements ChunkEmbeddingRepository {
	@PersistenceContext(unitName = "pgvector")
	private EntityManager em;

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
	public Optional<ChunkEmbedding> findByTenantIdAndChunkIdAndEmbeddingModel(Long tenantId, Long chunkId,
			String embeddingModel) {
		return jpaRepository.findByTenantIdAndChunkIdAndEmbeddingModel(tenantId, chunkId, embeddingModel)
				.map(mapper::toDomain);
	}

	@Override
	public List<ChunkEmbedding> findByTenantIdAndChunkIdInAndEmbeddingModel(Long tenantId, Collection<Long> chunkIds,
			String embeddingModel) {
		return jpaRepository.findByTenantIdAndChunkIdInAndEmbeddingModel(tenantId, chunkIds, embeddingModel).stream()
				.map(mapper::toDomain).toList();
	}

	@Override
	public long deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(Long tenantId, Long documentId,
			Integer documentVersion, String embeddingModel) {
		long deletedRows = jpaRepository.deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(tenantId, documentId, documentVersion, embeddingModel);
		return deletedRows;
	}

	@Override
	public long deleteByTenantIdAndChunkId(Long tenantId, Long chunkId) {
		return jpaRepository.deleteByTenantIdAndChunkId(tenantId, chunkId);
	}

	@Override
	public List<ChunkEmbedding> saveAll(List<ChunkEmbedding> embeddings) {
		return jpaRepository.saveAll(embeddings.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain)
				.toList();

	}
}
