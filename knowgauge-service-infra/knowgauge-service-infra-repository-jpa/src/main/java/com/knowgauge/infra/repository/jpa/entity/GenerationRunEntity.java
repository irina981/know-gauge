package com.knowgauge.infra.repository.jpa.entity;

import java.util.List;

import org.hibernate.annotations.Type;

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

	@Column(name = "test_id", nullable = false)
	private Long testId;

	@Column(nullable = false)
	private String model;

	@Column(name = "prompt_template_version", nullable = false)
	private String promptTemplateVersion;

	@Column(name = "retrieved_chunk_ids_json", columnDefinition = "jsonb")
	@Type(JsonType.class)
	private List<Long> retrievedChunkIds;

	@Column(name = "raw_response", columnDefinition = "TEXT")
	private String rawResponse;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GenerationRunStatus status;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;
}
