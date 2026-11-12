package com.knowgauge.infra.repository.jpa.entity;

import com.knowgauge.core.model.enums.QuestionStatus;

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
 * Question slot. Content lives in {@link TestQuestionVersionEntity} (ADR001).
 */
@Entity
@Table(name = "test_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestionEntity extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "test_id", nullable = false)
	private Long testId;

	@Column(name = "question_index", nullable = false)
	private Integer questionIndex;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private QuestionStatus status;

	@Column(name = "current_version_id")
	private Long currentVersionId;
}
