package com.knowgauge.infra.repository.jpa.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Type;

import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.model.enums.TestStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TestEntity extends AuditableEntity {
	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Builder.Default
	@ManyToMany
	@JoinTable(name = "test_covered_topics", joinColumns = @JoinColumn(name = "test_id"), inverseJoinColumns = @JoinColumn(name = "topic_id"))
	private List<TopicEntity> topics = new ArrayList<>();

	@Builder.Default
	@ManyToMany
	@JoinTable(name = "test_covered_documents", joinColumns = @JoinColumn(name = "test_id"), inverseJoinColumns = @JoinColumn(name = "document_id"))
	private List<DocumentEntity> documents = new ArrayList<>();

	@Builder.Default
	@ManyToMany
	@JoinTable(name = "test_used_chunks", joinColumns = @JoinColumn(name = "test_id"), inverseJoinColumns = @JoinColumn(name = "chunk_id"))
	private List<DocumentChunkEntity> usedChunks = new ArrayList<DocumentChunkEntity>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TestDifficulty difficulty;

	@Builder.Default
	@Column(name = "avoid_repeats", nullable = false)
	private boolean avoidRepeats = true;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(name = "coverage_mode", nullable = false)
	private TestCoverageMode coverageMode = TestCoverageMode.BALANCED_PER_DOC_CHUNKS;

	@Column(name = "question_count", nullable = false)
	private Integer questionCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TestStatus status;

	@Column(name = "generation_model")
	private String generationModel;

	@Column(name = "promptTemplateId")
	private String promptTemplateId;

	@Column(name = "generation_params_json", columnDefinition = "jsonb")
	@Type(JsonType.class)
	private Map<String, Object> generationParams;
}
