package com.knowgauge.core.service.content;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowgauge.core.context.ExecutionContext;
import com.knowgauge.core.exception.ResourceNotFoundException;
import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.TopicRepository;

/**
 * Topics of the own tenant and of the library (read-only unless the caller is an application admin, see
 * {@link ContentScope}). A sub-topic always lives in its parent's tenant.
 */
@Service
public class TopicServiceImpl implements TopicService {

	private final TopicRepository topicRepository;
	private final ExecutionContext executionContext;
	private final ContentScope contentScope;

	public TopicServiceImpl(TopicRepository topicRepository, ExecutionContext executionContext,
			ContentScope contentScope) {
		this.topicRepository = topicRepository;
		this.executionContext = executionContext;
		this.contentScope = contentScope;
	}

	@Override
	@Transactional
	public Topic create(Topic topic) {
		Long tenantId = executionContext.tenantId();
		Topic parent = null;
		if (topic.getParentId() != null) {
			parent = requireTopic(topic.getParentId(), "Parent topic not found: ");
			contentScope.requireWritable(parent.getTenantId(), "Topic " + parent.getId());
			tenantId = parent.getTenantId();
		} else if (topic.getTenantId() != null && topic.getTenantId().longValue() != tenantId.longValue()) {
			// A root topic in a shared library (ADR015): only a library, and only for whoever may change it (an ADMIN).
			if (!contentScope.isLibrary(topic.getTenantId())) {
				throw new IllegalArgumentException("Root topics can only be created in your own library or a shared one.");
			}
			contentScope.requireWritable(topic.getTenantId(), "Topic " + topic.getName());
			tenantId = topic.getTenantId();
		}
		validateUniqueNameUnderParent(tenantId, topic.getParentId(), topic.getName());

		topic.setTenantId(tenantId);

		Topic created = topicRepository.save(topic);

		// Update path of created topic
		String path = parent == null ? "/" + created.getId() : normalize(parent.getPath()) + created.getId();
		topicRepository.updatePath(tenantId, created.getId(), path);
		created.setPath(path);

		return created;
	}

	@Override
	@Transactional
	public Topic update(Topic topic) {
		Topic existingTopic = requireTopic(topic.getId(), "Topic not found: ");
		Long tenantId = existingTopic.getTenantId();
		contentScope.requireWritable(tenantId, "Topic " + topic.getId());

		// Only allow changing name + description (PATCH semantics: null = no change)
		String newName = topic.getName() != null ? topic.getName() : existingTopic.getName();
		String newDescription = topic.getDescription() != null ? topic.getDescription()
				: existingTopic.getDescription();

		// Enforce uniqueness only if name actually changes
		if (!existingTopic.getName().equals(newName)) {
			validateUniqueNameUnderParent(tenantId, existingTopic.getParentId(), newName);
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
		return topicRepository.findByTenantIdInAndParentId(contentScope.readableTenantIds(), parentId);
	}

	@Override
	@Transactional
	public List<Topic> getDescendants(Long rootId) {
		Topic topic = requireTopic(rootId, "Topic not found: ");
		// The subtree is in the root's tenant: a sub-topic always lives in its parent's tenant.
		return topicRepository.findByTenantIdInAndPathPrefix(List.of(topic.getTenantId()), normalize(topic.getPath()));
	}

	@Override
	@Transactional
	public List<Topic> getRoots() {
		return topicRepository.findByTenantIdInAndParentIdIsNull(contentScope.readableTenantIds());
	}

	@Override
	@Transactional
	public void delete(Long topicId) {
		Optional<Topic> topic = get(topicId);
		if (topic.isEmpty()) {
			return;
		}
		contentScope.requireWritable(topic.get().getTenantId(), "Topic " + topicId);
		topicRepository.deleteByTenantIdAndId(topic.get().getTenantId(), topicId);
	}

	@Override
	@Transactional
	public Optional<Topic> get(Long id) {
		return topicRepository.findByTenantIdInAndId(contentScope.readableTenantIds(), id);
	}

	private Topic requireTopic(Long topicId, String message) {
		return get(topicId).orElseThrow(() -> new ResourceNotFoundException(message + topicId));
	}

	private void validateUniqueNameUnderParent(Long tenantId, Long parentId, String name) {
		if (parentId == null) {
			if (topicRepository.existsByTenantIdAndParentIdIsNullAndName(tenantId, name)) {
				throw new IllegalArgumentException("Topic with name " + name + " already exists as a root topic.");
			}
			return;
		}

		if (topicRepository.existsByTenantIdAndParentIdAndName(tenantId, parentId, name)) {
			throw new IllegalArgumentException("Topic with name " + name + " already exists for parent id " + parentId);
		}
	}

	private String normalize(String path) {
		if (path == null || path.isBlank())
			return "/";
		return path.endsWith("/") ? path : path + "/";
	}

}
