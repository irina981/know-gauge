package com.knowgauge.contract.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record TopicTreeNodeInput(@NotBlank String name, String description, @NotEmpty List<TopicTreeNodeInput> children) {

}
