package com.knowgauge.repo.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "chunk_embeddings")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
public class ChunkEmbeddingEntity extends AuditableEntity {

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "embedding", columnDefinition = "vector")
    private float[] embedding;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;
}
