package com.knowgauge.contract.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicCreateInput(@NotBlank String name, String description, Long parentId) {
}
