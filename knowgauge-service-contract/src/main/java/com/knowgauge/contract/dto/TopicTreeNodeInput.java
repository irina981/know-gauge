package com.knowgauge.contract.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record TopicTreeNodeInput(@NotBlank String name, List<TopicTreeNodeInput> children) {

}
