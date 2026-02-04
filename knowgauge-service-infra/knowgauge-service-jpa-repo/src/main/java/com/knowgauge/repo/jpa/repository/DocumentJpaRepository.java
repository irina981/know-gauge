package com.knowgauge.repo.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.DocumentEntity;

@Repository
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {

	Page<DocumentEntity> findByTopicId(Long topicId, Pageable pageable);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update DocumentEntity d set d.storageKey = :storageKey where d.id = :id")
	void updateStorageKey(@Param("id") Long id, @Param("storageKey") String storageKey);
	
	boolean existsByTopicIdAndOriginalFileName(Long topicId, String originalFileName);

    boolean existsByTopicIdAndChecksum(Long topicId, String contentHash);
}