package com.knowgauge.core.service.content;

import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Topic;

public interface TopicService {
	public Optional<Topic> get(Long id);

	public Topic create(Topic req);
	
	public Topic update(Topic topic);
	
	public void delete(Long topicId);

	/**
	 * Creates many topics in one call (tree), under an existing parent.
	 */
	public Topic createTopicTree(Topic req);
	
	List<Topic> getChildren(Long parentId); // immediate children only
	
	List<Topic> getDescendants(Long rootId); // complete sub-tree
	
	List<Topic> getRoots();                 // immediate roots only

}
