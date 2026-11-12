package com.knowgauge.infra.repository.jpa.entity;

import java.util.List;

import org.hibernate.annotations.Type;

import com.knowgauge.core.model.enums.GenerationRunPurpose;
import com.knowgauge.core.model.enums.GenerationRunStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;

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

@Entity
@Table(name = "generation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GenerationRunEntity extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "test_id", nullable = false)
	private Long testId;

	@Column(name = "question_id")
	private Long questionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GenerationRunPurpose purpose;

	@Column(name = "batch_index")
	private Integer batchIndex;

	@Column(length = 100)
	private String model;

	@Column(name = "prompt_template_id", length = 100)
	private String promptTemplateId;

	@Column(name = "retrieved_chunk_ids_json", columnDefinition = "jsonb")
	@Type(JsonType.class)
	private List<Long> retrievedChunkIds;

	@Column(name = "raw_response", columnDefinition = "TEXT")
	private String rawResponse;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Column(name = "finish_reason", length = 50)
	private String finishReason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GenerationRunStatus status;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;
}
