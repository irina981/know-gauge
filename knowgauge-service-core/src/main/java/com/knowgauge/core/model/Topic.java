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
public class Topic {

	private Long id;

	private Long parentId;

	private String name;

	private String description;

	private String path;

	private Instant createdAt;

	private Instant updatedAt;

}
