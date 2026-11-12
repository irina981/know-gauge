package com.knowgauge.infra.repository.jpa.mapper;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.TestEvent;
import com.knowgauge.infra.repository.jpa.entity.TestEventEntity;

@Component
public class TestEventEntityMapper {

	public TestEventEntity toEntity(TestEvent domain) {
		return TestEventEntity.builder().id(domain.getId()).tenantId(domain.getTenantId()).testId(domain.getTestId())
				.questionId(domain.getQuestionId()).eventType(domain.getEventType())
				.fromVersionId(domain.getFromVersionId()).toVersionId(domain.getToVersionId())
				.actorUserId(domain.getActorUserId()).comment(domain.getComment())
				.payload(domain.getPayload() != null ? new LinkedHashMap<>(domain.getPayload()) : null)
				.createdAt(domain.getCreatedAt()).build();
	}

	public TestEvent toDomain(TestEventEntity entity) {
		return TestEvent.builder().id(entity.getId()).tenantId(entity.getTenantId()).testId(entity.getTestId())
				.questionId(entity.getQuestionId()).eventType(entity.getEventType())
				.fromVersionId(entity.getFromVersionId()).toVersionId(entity.getToVersionId())
				.actorUserId(entity.getActorUserId()).comment(entity.getComment()).payload(entity.getPayload())
				.createdAt(entity.getCreatedAt()).build();
	}
}
