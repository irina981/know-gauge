package com.knowgauge.repo.jpa.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
