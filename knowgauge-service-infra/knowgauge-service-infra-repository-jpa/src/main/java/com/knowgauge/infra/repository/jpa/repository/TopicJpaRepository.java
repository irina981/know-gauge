package com.knowgauge.infra.repository.jpa.repository;

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

	List<TopicEntity> findByParentId(Long parentId);

	boolean existsByParentIdAndName(Long parentId, String name);
	
	boolean existsByParentIdIsNullAndName(String name);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update TopicEntity t set t.path = :path where t.id = :id")
	void updatePath(@Param("id") Long id, @Param("path") String path);
	
	// roots
    List<TopicEntity> findByParentIdIsNull();

    // get path for a topic (avoid loading full row)
    @Query("select t.path from TopicEntity t where t.id = :id")
    Optional<String> findPathById(@Param("id") Long id);

    // subtree by path prefix
    @Query("select t from TopicEntity t where t.path like concat(:prefix, '%')")
    List<TopicEntity> findByPathPrefix(@Param("prefix") String prefix);
}