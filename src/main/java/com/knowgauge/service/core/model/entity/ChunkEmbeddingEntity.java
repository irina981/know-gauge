package com.knowgauge.service.core.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chunk_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkEmbeddingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "embedding", columnDefinition = "vector")
    private float[] embedding;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
