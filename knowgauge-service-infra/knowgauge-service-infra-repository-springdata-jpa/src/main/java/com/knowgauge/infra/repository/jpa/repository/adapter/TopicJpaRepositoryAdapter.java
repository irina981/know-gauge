package com.knowgauge.infra.repository.jpa.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.knowgauge.core.model.Topic;
import com.knowgauge.core.port.repository.TopicRepository;
import com.knowgauge.infra.repository.jpa.entity.TopicEntity;
import com.knowgauge.infra.repository.jpa.mapper.TopicEntityMapper;
import com.knowgauge.infra.repository.jpa.repository.TopicJpaRepository;

@Repository
public class TopicJpaRepositoryAdapter implements TopicRepository {

	private final TopicJpaRepository jpaRepository;
	private final TopicEntityMapper mapper;

	public TopicJpaRepositoryAdapter(TopicJpaRepository jpaRepository, TopicEntityMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Topic save(Topic domain) {
		TopicEntity entity = mapper.toEntity(domain);
		TopicEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public void deleteByTenantIdAndId(Long tenantId, Long topicId) {
		jpaRepository.deleteByTenantIdAndId(tenantId, topicId);
	}

	@Override
	public void updatePath(Long tenantId, Long id, String path) {
		jpaRepository.updatePath(tenantId, id, path);
	}

	@Override
	public Optional<Topic> findByTenantIdInAndId(Collection<Long> tenantIds, Long id) {
		return jpaRepository.findByTenantIdInAndId(tenantIds, id).map(mapper::toDomain);
	}

	@Override
	public List<Topic> findByTenantIdInAndParentId(Collection<Long> tenantIds, Long parentId) {
		return jpaRepository.findByTenantIdInAndParentId(tenantIds, parentId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public boolean existsByTenantIdAndParentIdAndName(Long tenantId, Long parentId, String name) {
		return jpaRepository.existsByTenantIdAndParentIdAndName(tenantId, parentId, name);
	}

	@Override
	public boolean existsByTenantIdAndParentIdIsNullAndName(Long tenantId, String name) {
		return jpaRepository.existsByTenantIdAndParentIdIsNullAndName(tenantId, name);
	}

	@Override
	public List<Topic> findByTenantIdInAndParentIdIsNull(Collection<Long> tenantIds) {
		return jpaRepository.findByTenantIdInAndParentIdIsNull(tenantIds).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Topic> findByTenantIdInAndPathPrefix(Collection<Long> tenantIds, String prefix) {
		return jpaRepository.findByTenantIdInAndPathPrefix(tenantIds, prefix).stream().map(mapper::toDomain).toList();
	}
}
