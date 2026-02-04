package com.knowgauge.core.service.content;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.TopicRepository;

@Service
public class TopicServiceImpl implements TopicService {

	private final TopicRepository topicRepository;

	public TopicServiceImpl(TopicRepository topicRepository) {
		this.topicRepository = topicRepository;
	}

	@Override
	@Transactional
	public Topic create(Topic topic) {
		validateUniqueNameUnderParent(topic.getParentId(), topic.getName());

		Topic created = topicRepository.save(topic);

		// Update path of created topic
		String path = buildPath(created.getId(), created.getParentId());
		updatePath(created.getId(), path);
		created.setPath(path);

		return created;
	}
	
	private void updatePath(Long topicId, String path) {
		topicRepository.updatePath(topicId, path);
	}

	@Override
	@Transactional
	public Topic update(Topic topic) {
		Topic existingTopic = topicRepository.findById(topic.getId())
				.orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topic.getId()));

		// Only allow changing name + description (PATCH semantics: null = no change)
		String newName = topic.getName() != null ? topic.getName() : existingTopic.getName();
		String newDescription = topic.getDescription() != null ? topic.getDescription()
				: existingTopic.getDescription();

		// Enforce uniqueness only if name actually changes
		if (existingTopic.getParentId() != null && !existingTopic.getName().equals(newName)) {
			validateUniqueNameUnderParent(existingTopic.getParentId(), newName);
		}

		existingTopic.setName(newName);
		existingTopic.setDescription(newDescription);

		// Save the FULL object (safe: no null-wiping)
		return topicRepository.save(existingTopic);
	}

	@Override
	@Transactional
	public Topic createTopicTree(Topic rootTopic) {
	    // For a "tree create", we treat the provided req as the root.
	    // req.parentId may be null (most common) or could be set if you're attaching under an existing parent.
	    Topic createdRootTopic = create(rootTopic);

	    // Recursively create children (if any)
	    if (rootTopic.getChildren() != null && !rootTopic.getChildren().isEmpty()) {
	        List<Topic> createdChildren = createChildrenRecursive(createdRootTopic.getId(), rootTopic.getChildren());
	        createdRootTopic.setChildren(createdChildren);
	    } else {
	    	createdRootTopic.setChildren(List.of());
	    }

	    return createdRootTopic;
	}
	
	/**
	 * Recursively creates children under the given parentId, preserving the input structure.
	 */
	private List<Topic> createChildrenRecursive(Long parentId, List<Topic> childrenReq) {
	    List<Topic> result = new java.util.ArrayList<>(childrenReq.size());

	    for (Topic childReq : childrenReq) {
	        if (childReq == null) {
	            continue;
	        }
	        if (childReq.getName() == null || childReq.getName().isBlank()) {
	            throw new IllegalArgumentException("Child topic name is required (parentId=" + parentId + ")");
	        }

	        Topic createdChild = create(new Topic(parentId, childReq.getName(), childReq.getDescription()));

	        if (childReq.getChildren() != null && !childReq.getChildren().isEmpty()) {
				createdChild.setChildren(createChildrenRecursive(createdChild.getId(), childReq.getChildren()));
	        } else {
	            createdChild.setChildren(List.of());
	        }

	        result.add(createdChild);
	    }

	    return result;
	}

	@Override
	@Transactional
	public List<Topic> getChildren(Long parentId) {
		return topicRepository.findByParentId(parentId);
	}

	@Override
	@Transactional
	public List<Topic> getDescendants(Long rootId) {
		Topic topic = topicRepository.findById(rootId)
				.orElseThrow(() -> new IllegalArgumentException("Topic not found: " + rootId));
		String path =  normalize(topic.getPath());
		return topicRepository.findByPathPrefix(path);
	}

	@Override
	@Transactional
	public List<Topic> getRoots() {
		return topicRepository.findByParentIdIsNull();
	}

	@Override
	@Transactional
	public void delete(Long topicId) {
		topicRepository.delete(topicId);

	}

	@Override
	@Transactional
	public Optional<Topic> get(Long id) {
		return topicRepository.findById(id);
	}

	private String buildPath(Long topicId, Long parentId) {

		if (topicId == null) {
			throw new IllegalArgumentException("topicId must be set");
		}

		if (parentId == null) {
			return "/" + topicId;
		}

		Topic parent = topicRepository.findById(parentId)
				.orElseThrow(() -> new IllegalArgumentException("Parent topic not found: " + parentId));

		return normalize(parent.getPath()) + topicId;
	}

	private void validateUniqueNameUnderParent(Long parentId, String name) {
		if (parentId == null) {
			if (topicRepository.existsByParentIdIsNullAndName(name)) {
				throw new IllegalArgumentException("Topic with name " + name + " already exists as a root topic.");
			}
		}

		if (topicRepository.existsByParentIdAndName(parentId, name)) {
			throw new IllegalArgumentException("Topic with name " + name + " already exists for parent id " + parentId);
		}
	}

	private String normalize(String path) {
		if (path == null || path.isBlank())
			return "/";
		return path.endsWith("/") ? path : path + "/";
	}

}
