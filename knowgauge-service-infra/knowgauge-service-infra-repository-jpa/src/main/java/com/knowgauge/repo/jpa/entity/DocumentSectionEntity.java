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
@Table(name = "document_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentSectionEntity extends AuditableEntity {

	@Column(name = "document_id", nullable = false)
	private Long documentId;

	@Column(nullable = false)
	private String title;

	@Column(name = "order_index", nullable = false)
	private Integer orderIndex;

	@Column(name = "start_page")
	private Integer startPage;

	@Column(name = "end_page")
	private Integer endPage;
}
