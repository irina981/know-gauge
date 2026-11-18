package com.knowgauge.infra.repository.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.knowgauge.infra.repository.jpa.entity.TopicEntity;

@Repository
public interface TopicJpaRepository extends JpaRepository<TopicEntity, Long> {

	Optional<TopicEntity> findByTenantIdInAndId(Collection<Long> tenantIds, Long id);

	List<TopicEntity> findByTenantIdInAndParentId(Collection<Long> tenantIds, Long parentId);

	boolean existsByTenantIdAndParentIdAndName(Long tenantId, Long parentId, String name);

	boolean existsByTenantIdAndParentIdIsNullAndName(Long tenantId, String name);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update TopicEntity t set t.path = :path where t.tenantId = :tenantId and t.id = :id")
	void updatePath(@Param("tenantId") Long tenantId, @Param("id") Long id, @Param("path") String path);

	// Subtopics and their documents go with it (ON DELETE CASCADE).
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from TopicEntity t where t.tenantId = :tenantId and t.id = :id")
	int deleteByTenantIdAndId(@Param("tenantId") Long tenantId, @Param("id") Long id);

	// roots
	List<TopicEntity> findByTenantIdInAndParentIdIsNull(Collection<Long> tenantIds);

	// subtree by path prefix
	@Query("select t from TopicEntity t where t.tenantId in :tenantIds and t.path like concat(:prefix, '%')")
	List<TopicEntity> findByTenantIdInAndPathPrefix(@Param("tenantIds") Collection<Long> tenantIds,
			@Param("prefix") String prefix);
}
