package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.knowgauge.core.model.Topic;
import com.knowgauge.repo.jpa.entity.TopicEntity;

@Mapper(componentModel = "spring")
public interface TopicEntityMapper {
	Topic toDomain(TopicEntity entity);

	TopicEntity toEntity(Topic domain);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(Topic domain, @MappingTarget TopicEntity entity);
}