package com.knowgauge.core.port.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.knowgauge.core.model.Topic;

/**
 * Topics, always filtered by tenant. Reads take the tenants whose content may be read (the own tenant and the
 * library, see {@code ContentScope}); writes and name checks take the one tenant written to.
 */
public interface TopicRepository {
	Topic save(Topic domain);

	void deleteByTenantIdAndId(Long tenantId, Long topicId);

	Optional<Topic> findByTenantIdInAndId(Collection<Long> tenantIds, Long id);

	List<Topic> findByTenantIdInAndParentId(Collection<Long> tenantIds, Long parentId);

	boolean existsByTenantIdAndParentIdAndName(Long tenantId, Long parentId, String name);

	boolean existsByTenantIdAndParentIdIsNullAndName(Long tenantId, String name);

	void updatePath(Long tenantId, Long id, String path);

	// roots
	List<Topic> findByTenantIdInAndParentIdIsNull(Collection<Long> tenantIds);

	// subtree by path prefix
	List<Topic> findByTenantIdInAndPathPrefix(Collection<Long> tenantIds, String prefix);
}
