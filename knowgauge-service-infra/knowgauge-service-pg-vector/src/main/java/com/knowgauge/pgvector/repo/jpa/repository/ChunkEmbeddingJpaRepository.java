package com.knowgauge.pgvector.repo.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.pgvector.repo.jpa.entity.ChunkEmbeddingEntity;

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

	/**
	 * Explicit JPQL delete instead of derived delete method.
	 *
	 * IMPORTANT: In this multi-EntityManager setup (separate pgvector persistence
	 * unit), the derived delete method:
	 *
	 * deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(...)
	 *
	 * was executed via a ResultSet-based execution path (as if it were a SELECT),
	 * which caused PostgreSQL to throw:
	 *
	 * "No results were returned by the query"
	 *
	 * Using @Modifying + explicit JPQL forces Spring Data JPA to execute the
	 * statement using executeUpdate() instead of executeQuery(), ensuring correct
	 * DELETE semantics with the pgvector persistence unit.
	 *
	 * This is required for stable behavior in the secondary (vector) datasource.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			  delete from ChunkEmbeddingEntity e
			  where e.tenantId = :tenantId
			    and e.documentId = :documentId
			    and e.documentVersion = :documentVersion
			    and e.embeddingModel = :embeddingModel
			""")
	int deleteByTenantIdAndDocumentIdAndDocumentVersionAndEmbeddingModel(@Param("tenantId") Long tenantId,
			@Param("documentId") Long documentId, @Param("documentVersion") Integer documentVersion,
			@Param("embeddingModel") String embeddingModel);

	/**
	 * Same reasoning as above: explicit JPQL + @Modifying ensures the DELETE is
	 * executed using executeUpdate() within the pgvector persistence unit.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    delete from ChunkEmbeddingEntity e
			    where e.tenantId = :tenantId
			      and e.chunkId = :chunkId
			""")
	long deleteByTenantIdAndChunkId(@Param("tenantId") Long tenantId, @Param("chunkId") Long chunkId);

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
