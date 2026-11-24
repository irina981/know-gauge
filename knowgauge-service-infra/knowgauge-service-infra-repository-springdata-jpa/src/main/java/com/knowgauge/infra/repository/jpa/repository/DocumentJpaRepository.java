package com.knowgauge.infra.repository.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.enums.DocumentStatus;
import com.knowgauge.infra.repository.jpa.entity.DocumentEntity;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {

	Optional<DocumentEntity> findByTenantIdInAndId(Collection<Long> tenantIds, Long id);

	Page<DocumentEntity> findByTenantIdInAndTopicId(Collection<Long> tenantIds, Long topicId, Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update DocumentEntity d set d.storageKey = :storageKey where d.tenantId = :tenantId and d.id = :id")
	void updateStorageKey(@Param("tenantId") Long tenantId, @Param("id") Long id,
			@Param("storageKey") String storageKey);

	@Modifying
	@Query("""
			update DocumentEntity d
			set d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTING
			where d.tenantId = :tenantId
			  and d.id = :documentId
			  and (d.status = com.knowgauge.core.model.enums.DocumentStatus.UPLOADED or d.status = com.knowgauge.core.model.enums.DocumentStatus.FAILED)
			""")
	int markIngesting(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update DocumentEntity d
			       set d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTED,
			           d.ingestedAt = CURRENT_TIMESTAMP,
			           d.errorMessage = null
			     where d.tenantId = :tenantId
			       and d.id = :documentId
			       and d.status = com.knowgauge.core.model.enums.DocumentStatus.INGESTING
			""")
	int markIngested(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			    update DocumentEntity d
			       set d.status = com.knowgauge.core.model.enums.DocumentStatus.FAILED,
			           d.errorMessage = :errorMessage
			     where d.tenantId = :tenantId
			       and d.id = :documentId
			""")
	int markFailed(@Param("tenantId") Long tenantId, @Param("documentId") Long documentId,
			@Param("errorMessage") String errorMessage);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from DocumentEntity d where d.tenantId = :tenantId and d.id = :id")
	int deleteByTenantIdAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

	boolean existsByTenantIdAndTopicIdAndOriginalFileName(Long tenantId, Long topicId, String originalFileName);

	boolean existsByTenantIdAndTopicIdAndChecksum(Long tenantId, Long topicId, String checksum);

	@Query("select coalesce(sum(d.fileSizeBytes), 0) from DocumentEntity d where d.tenantId = :tenantId")
	long sumFileSizeBytesByTenantId(@Param("tenantId") Long tenantId);

	@Query("""
			select d.topicId, count(d) from DocumentEntity d
			where d.tenantId in :tenantIds and d.status = :status
			group by d.topicId
			""")
	List<Object[]> countByTopicId(@Param("tenantIds") Collection<Long> tenantIds, @Param("status") DocumentStatus status);

	List<DocumentEntity> findByTenantIdInAndTopicIdIn(Collection<Long> tenantIds, List<Long> topicIds);

	List<DocumentEntity> findByTenantIdAndStatusOrderByCreatedAtAscIdAsc(Long tenantId, DocumentStatus status,
			Pageable pageable);

	@Query("select distinct d.tenantId from DocumentEntity d where d.status = :status order by d.tenantId")
	List<Long> findTenantIdsByStatus(@Param("status") DocumentStatus status);
}
