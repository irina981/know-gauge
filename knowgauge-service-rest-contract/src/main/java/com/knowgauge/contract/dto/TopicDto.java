package com.knowgauge.contract.dto;

import java.util.List;

public record TopicDto(Long id, String name, String description, Long parentId, String path, List<TopicDto> children) {
}
