package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Topic details")
public record TopicDto(
		@Schema(description = "Topic id", example = "101") Long id,
		@Schema(description = "Topic name", example = "Spring Boot") String name,
		@Schema(description = "Topic description", example = "Core concepts and practical usage") String description,
		@Schema(description = "Parent topic id", example = "10") Long parentId,
		@Schema(description = "Materialized topic path", example = "/Backend/Spring Boot") String path,
		@Schema(description = "Display order among sibling topics (lowest first); empty sorts after positioned topics, by name", example = "1") Integer position,
		@Schema(description = "True when the caller may read but not change the topic (shared library content): hide edit actions, keep test generation") boolean readOnly,
		@Schema(description = "True when the topic belongs to the shared library (a LIBRARY tenant), whoever is asking") boolean shared,
		@Schema(description = "Direct child topics") List<TopicDto> children) {
}
