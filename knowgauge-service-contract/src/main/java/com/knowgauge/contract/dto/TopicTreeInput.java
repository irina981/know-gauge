package com.knowgauge.contract.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TopicTreeInput(@NotNull Long parentId, @NotNull TopicTreeNodeInput rootNode) {
}