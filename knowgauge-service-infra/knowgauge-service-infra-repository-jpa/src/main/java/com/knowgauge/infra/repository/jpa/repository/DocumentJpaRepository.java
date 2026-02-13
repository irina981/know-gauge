package com.knowgauge.infra.repository.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.DocumentEntity;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {

	Page<DocumentEntity> findByTopicId(Long topicId, Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update DocumentEntity d set d.storageKey = :storageKey where d.id = :id")
	void updateStorageKey(@Param("id") Long id, @Param("storageKey") String storageKey);

	@Modifying
	@Query("""
			update DocumentEntity d
			set d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTING
			where d.id = :documentId 
			  and (d.status = com.knowgauge.core.model.enums.DocumentStatus.UPLOADED or d.status = com.knowgauge.core.model.enums.DocumentStatus.FAILED)
			""")
	int markIngesting(@Param("documentId") Long documentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update DocumentEntity d
			       set d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTED,
			           d.ingestedAt = CURRENT_TIMESTAMP,
			           d.errorMessage = null
			     where d.id = :documentId
			       and d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTING
			""")
	int markIngested(@Param("documentId") Long documentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update DocumentEntity d
			       set d.status = com.knowgauge.core.model.enums.DocumentStatus.FAILED,
			           d.errorMessage = :errorMessage
			     where d.id = :documentId
			""")
	int markFailed(@Param("documentId") Long documentId, @Param("errorMessage") String errorMessage);

	boolean existsByTopicIdAndOriginalFileName(Long topicId, String originalFileName);

	boolean existsByTopicIdAndChecksum(Long topicId, String contentHash);
}