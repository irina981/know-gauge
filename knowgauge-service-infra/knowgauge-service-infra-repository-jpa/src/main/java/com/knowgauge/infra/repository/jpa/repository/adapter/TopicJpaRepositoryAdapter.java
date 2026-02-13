package com.knowgauge.infra.repository.jpa.repository.adapter;

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
	public void delete (Long topicId) {
		jpaRepository.deleteById(topicId);
	}

	@Override
	public void updatePath(Long id, String path) {
		jpaRepository.updatePath(id, path);
	}

	@Override
	public Optional<Topic> findById(Long id) {
		return jpaRepository.findById(id).map(mapper::toDomain);
	}

	@Override
	public List<Topic> findByParentId(Long parentId) {
		return jpaRepository.findByParentId(parentId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public boolean existsByParentIdAndName(Long parentId, String name) {
		return jpaRepository.existsByParentIdAndName(parentId, name);
	}

	@Override
	public List<Topic> findByParentIdIsNull() {
		return jpaRepository.findByParentIdIsNull().stream().map(mapper::toDomain).toList();
	}

	@Override
	public Optional<String> findPathById(Long id) {
		return jpaRepository.findPathById(id);
	}

	@Override
	public List<Topic> findByPathPrefix(String prefix) {
		return jpaRepository.findByPathPrefix(prefix).stream().map(mapper::toDomain).toList();
	}

	@Override
	public boolean existsByParentIdIsNullAndName(String name) {
		return jpaRepository.existsByParentIdIsNullAndName(name);
	}
	
	
}
