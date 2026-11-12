package com.knowgauge.infra.repository.jpa.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import com.knowgauge.core.model.enums.VersionOrigin;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Immutable question content snapshot (ADR001). Insert-only: Hibernate never
 * issues updates for it.
 */
@Entity
@Immutable
@Table(name = "test_question_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestQuestionVersionEntity extends PersistedEntity {

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "question_id", nullable = false)
	private Long questionId;

	@Column(name = "version_no", nullable = false)
	private Integer versionNo;

	@Enumerated(EnumType.STRING)
	@Column(name = "origin", nullable = false, length = 20)
	private VersionOrigin origin;

	@Column(name = "parent_version_id")
	private Long parentVersionId;

	@Column(name = "generation_run_id")
	private Long generationRunId;

	@Column(name = "verification_run_id")
	private Long verificationRunId;

	@Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
	private String questionText;

	@Column(columnDefinition = "TEXT")
	private String explanation;

	@Column(name = "source_chunk_ids_json", columnDefinition = "jsonb")
	@Type(JsonType.class)
	private List<Long> sourceChunkIds;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Null = system. */
	@Column(name = "created_by", updatable = false)
	private Long createdBy;

	@OneToMany(mappedBy = "version", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@OrderBy("letter ASC")
	@Builder.Default
	private List<TestQuestionVersionOptionEntity> options = new ArrayList<>();
}
