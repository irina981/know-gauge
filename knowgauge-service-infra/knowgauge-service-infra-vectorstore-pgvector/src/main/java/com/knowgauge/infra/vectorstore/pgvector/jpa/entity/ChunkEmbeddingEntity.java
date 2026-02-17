package com.knowgauge.infra.vectorstore.pgvector.jpa.entity;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "chunk_id", nullable = false)
	private Long chunkId;

	/**
	 * Duplicated metadata so vector search can be filtered without joining
	 * document_chunks (future vector DB).
	 */
	@Column(name = "document_id", nullable = false)
	private Long documentId;

	@Column(name = "document_version", nullable = false)
	private Integer documentVersion;

	@Column(name = "topic_id", nullable = false)
	private Long topicId;

	@Column(name = "section_id")
	private Long sectionId;

	/**
	 * Must match document_chunks.checksum. Allows drift detection if chunks are
	 * re-generated.
	 */
	@Column(name = "chunk_checksum", nullable = false, length = 64)
	private String chunkChecksum;

	@Column(name = "embedding", columnDefinition = "vector")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
	private float[] embedding;

	@Column(name = "embedding_model", nullable = false, length = 100)
	private String embeddingModel;
}
