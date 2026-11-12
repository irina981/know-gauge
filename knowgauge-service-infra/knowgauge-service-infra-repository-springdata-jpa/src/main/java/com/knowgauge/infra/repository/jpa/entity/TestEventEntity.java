package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import com.knowgauge.core.model.enums.TestEventType;
import io.hypersistence.utils.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Append-only history entry (ADR001).
 */
@Entity
@Immutable
@Table(name = "test_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestEventEntity extends PersistedEntity {

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "test_id", nullable = false)
	private Long testId;

	@Column(name = "question_id")
	private Long questionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private TestEventType eventType;

	@Column(name = "from_version_id")
	private Long fromVersionId;

	@Column(name = "to_version_id")
	private Long toVersionId;

	@Column(name = "actor_user_id")
	private Long actorUserId;

	@Column(columnDefinition = "TEXT")
	private String comment;

	@Column(name = "payload_json", columnDefinition = "jsonb")
	@Type(JsonType.class)
	private Map<String, Object> payload;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
