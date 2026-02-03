package com.knowgauge.core.port.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;

import com.knowgauge.core.model.Topic;

public interface TopicRepository {
	public Topic save(Topic domain);
	
	public void delete (Long topicId);
	
	public Optional<Topic> findById(Long id);

	List<Topic> findByParentId(Long parentId);

	boolean existsByParentIdAndName(Long parentId, String name);
	
	boolean existsByParentIdIsNullAndName(String name);

	void updatePath(Long id, String path);

	// roots
	List<Topic> findByParentIdIsNull();

	Optional<String> findPathById(Long id);

	// subtree by path prefix
	List<Topic> findByPathPrefix(@Param("prefix") String prefix);

}