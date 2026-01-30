package com.knowgauge.repo.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.knowgauge.repo.jpa.entity.TopicEntity;

@Repository
public interface TopicJpaRepository extends JpaRepository<TopicEntity, Long> {

	List<TopicEntity> findByParentId(Long parentId);
}