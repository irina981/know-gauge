package com.knowgauge.contract.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicInput(@NotBlank String name, Long parentId) {
}
