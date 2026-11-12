package com.knowgauge.core.model;

import java.time.Instant;
import java.util.Map;

import com.knowgauge.core.model.enums.TestEventType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One entry of the append-only history trail of a test and its questions (see
 * ADR001). Events are never updated or deleted (except by cascade when the test
 * is deleted).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestEvent extends PersistedObject {

	private Long tenantId;

	private Long testId;

	/** Null for test-level events. */
	private Long questionId;

	private TestEventType eventType;

	private Long fromVersionId;

	private Long toVersionId;

	/** Null means system. */
	private Long actorUserId;

	private String comment;

	/** Event-specific details (e.g. flagged option letter, reason, suggestion). */
	private Map<String, Object> payload;

	private Instant createdAt;
}
