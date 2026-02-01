package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Topic;

public interface TopicRepository {
	Topic save(Topic topic);

	Optional<Topic> findById(Long id);

	List<Topic> findByParentId(Long parentId);
	
	boolean existsByParentIdAndName(Long parentId, String name);
}