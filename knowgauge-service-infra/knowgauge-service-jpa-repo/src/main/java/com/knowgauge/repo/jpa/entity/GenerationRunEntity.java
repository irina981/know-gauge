package com.knowgauge.repo.jpa.entity;

import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.Type;

import com.knowgauge.core.model.enums.GenerationRunStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "generation_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
