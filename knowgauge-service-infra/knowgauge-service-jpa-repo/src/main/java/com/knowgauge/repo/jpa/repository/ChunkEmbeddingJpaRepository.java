package com.knowgauge.repo.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.ChunkEmbeddingEntity;

@Repository
public interface ChunkEmbeddingJpaRepository extends JpaRepository<ChunkEmbeddingEntity, Long> {

	// -------------------------------------------------
	// Single embedding lookup
	// -------------------------------------------------

	Optional<ChunkEmbeddingEntity> findByTenantIdAndChunkIdAndEmbeddingModel(Long tenantId, Long chunkId,
			String embeddingModel);

	// -------------------------------------------------
	// Bulk lookup by chunk ids (e.g. re-embedding checks)
	// -------------------------------------------------

	List<ChunkEmbeddingEntity> findByTenantIdAndChunkIdInAndEmbeddingModel(Long tenantId, Collection<Long> chunkIds,
			String embeddingModel);

	// -------------------------------------------------
	// Delete on re-ingestion / re-embedding
	// -------------------------------------------------

	void deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(Long tenantId, Long documentId,
			Integer documentVersion, String embeddingModel);

	void deleteByTenantIdAndChunkId(Long tenantId, Long chunkId);

	// -------------------------------------------------
	// Optional: drift / consistency checks
	// -------------------------------------------------

	List<ChunkEmbeddingEntity> findByTenantIdAndChunkChecksumAndEmbeddingModel(Long tenantId, String chunkChecksum,
			String embeddingModel);

	// -------------------------------------------------
	// Optional: admin / debug
	// -------------------------------------------------

	long countByTenantIdAndEmbeddingModel(Long tenantId, String embeddingModel);
}
