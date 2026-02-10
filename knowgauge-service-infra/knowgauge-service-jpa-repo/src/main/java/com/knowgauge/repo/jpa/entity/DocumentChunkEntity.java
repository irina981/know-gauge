package com.knowgauge.repo.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentChunkEntity extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_version", nullable = false)
    private Integer documentVersion;

    /**
     * Topic at ingestion time (denormalized for fast filtering).
     * Document is the real parent.
     */
    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    /**
     * Optional, nullable for now (you decided to defer sections).
     */
    @Column(name = "section_id")
    private Long sectionId;

    /**
     * Global ordinal within (tenant, document, version).
     * Renamed from chunk_index → ordinal for clarity.
     */
    @Column(name = "ordinal", nullable = false)
    private Integer ordinal;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "start_page")
    private Integer startPage;

    @Column(name = "end_page")
    private Integer endPage;

    /**
     * Character offsets relative to the page (if per-page chunking).
     */
    @Column(name = "char_start")
    private Integer charStart;

    @Column(name = "char_end")
    private Integer charEnd;

    /**
     * SHA-256 (hex) of chunk_text.
     */
    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

// TODO: add later
//    /**
//     * Optional but highly recommended:
//     * lets you detect how chunks were produced.
//     */
//    @Column(name = "splitter_type", nullable = false, length = 50)
//    private String splitterType;
//
//    /**
//     * Hash of splitter configuration (chunk size, overlap, normalization version, etc.)
//     */
//    @Column(name = "splitter_config_hash", nullable = false, length = 64)
//    private String splitterConfigHash;
}
