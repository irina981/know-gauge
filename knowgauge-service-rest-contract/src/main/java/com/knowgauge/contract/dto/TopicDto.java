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
		@Schema(description = "Direct child topics") List<TopicDto> children) {
}
