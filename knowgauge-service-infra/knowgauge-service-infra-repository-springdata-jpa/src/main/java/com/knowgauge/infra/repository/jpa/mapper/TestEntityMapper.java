package com.knowgauge.infra.repository.jpa.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.knowgauge.core.model.Test;
import com.knowgauge.infra.repository.jpa.entity.DocumentEntity;
import com.knowgauge.infra.repository.jpa.entity.TestEntity;
import com.knowgauge.infra.repository.jpa.entity.TopicEntity;
import com.knowgauge.infra.repository.jpa.repository.DocumentJpaRepository;
import com.knowgauge.infra.repository.jpa.repository.TopicJpaRepository;

@Mapper(componentModel = "spring")
public abstract class TestEntityMapper {

    @Autowired
    protected TopicJpaRepository topicRepo;

    @Autowired
    protected DocumentJpaRepository documentRepo;

    @Mapping(target = "topicIds", source = "topics")
    @Mapping(target = "documentIds", source = "documents")
    public abstract Test toDomain(TestEntity entity);

    @Mapping(target = "topics", source = "topicIds")
    @Mapping(target = "documents", source = "documentIds")
    public abstract TestEntity toEntity(Test domain);

    // -------- entity -> id --------
    protected List<Long> topicsToIds(List<TopicEntity> topics) {
        if (topics == null) return null;
        return topics.stream().map(TopicEntity::getId).toList();
    }

    protected List<Long> documentsToIds(List<DocumentEntity> docs) {
        if (docs == null) return null;
        return docs.stream().map(DocumentEntity::getId).toList();
    }

    // -------- id -> entity (use references) --------
    protected List<TopicEntity> idsToTopics(List<Long> ids) {
        if (ids == null) return null;
        return ids.stream()
                .map(id -> topicRepo.getReferenceById(id))
                .toList();
    }

    protected List<DocumentEntity> idsToDocuments(List<Long> ids) {
        if (ids == null) return null;
        return ids.stream()
                .map(id -> documentRepo.getReferenceById(id))
                .toList();
    }
}
