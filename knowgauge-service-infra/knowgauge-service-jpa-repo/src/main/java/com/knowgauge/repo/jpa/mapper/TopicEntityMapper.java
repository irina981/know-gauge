package com.knowgauge.repo.jpa.mapper;

import org.mapstruct.Mapper;
import com.knowgauge.repo.jpa.entity.TopicEntity;
import com.knowgauge.core.model.Topic;

@Mapper(componentModel = "spring")
public interface TopicEntityMapper {
    Topic toDomain(TopicEntity entity);
    TopicEntity toEntity(Topic domain);
}