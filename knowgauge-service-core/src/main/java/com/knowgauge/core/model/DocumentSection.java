package com.knowgauge.core.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSection {

	private Long id;

	private Long documentId;

	private String title;

	private Integer orderIndex;

	private Integer startPage;

	private Integer endPage;

	private Instant createdAt;

}
