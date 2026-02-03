package com.knowgauge.contract.dto;

import jakarta.validation.constraints.NotBlank;

public record TopicUpdateInput(Long id, @NotBlank String name, String description) {
}
