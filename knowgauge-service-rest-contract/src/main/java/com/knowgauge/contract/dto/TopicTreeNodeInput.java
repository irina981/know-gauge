package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Recursive node used to create a topic tree")
public record TopicTreeNodeInput(
		@NotBlank @Schema(description = "Node name", example = "Backend") String name,
		@Schema(description = "Optional node description", example = "Service-side implementation topics") String description,
		@NotEmpty @Schema(description = "Child nodes", example = "[{\"name\":\"Spring\",\"description\":\"Core Spring modules\",\"children\":[{\"name\":\"Spring Boot\",\"description\":\"Auto-configuration and starters\",\"children\":[]}]}]") List<TopicTreeNodeInput> children) {

}
