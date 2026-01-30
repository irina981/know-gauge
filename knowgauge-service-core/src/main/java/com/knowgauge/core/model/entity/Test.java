package com.knowgauge.core.model.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.model.enums.TestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "topic_id", nullable = false)
	private Long topicId;

	@Builder.Default
	@ManyToMany
	@JoinTable(name = "test_used_chunks", joinColumns = @JoinColumn(name = "test_id"), inverseJoinColumns = @JoinColumn(name = "chunk_id"))
	private Set<DocumentChunk> usedChunks = new HashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TestDifficulty difficulty;
	
	@Builder.Default
	@Column(name = "avoid_repeats", nullable = false)
    private boolean avoidRepeats = true;

	@Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_mode", nullable = false)
    private TestCoverageMode coverageMode = TestCoverageMode.BALANCED;

	@Column(name = "question_count", nullable = false)
	private Integer questionCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TestStatus status;

	@Column(name = "generation_model")
	private String generationModel;

	@Column(name = "generation_params_json", columnDefinition = "TEXT")
	private String generationParamsJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}
}
