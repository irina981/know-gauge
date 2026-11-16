package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "How many ingested documents a topic holds directly (sub-topics are counted separately)")
public record TopicDocumentCountDto(
		@Schema(description = "Topic id", example = "9100") Long topicId,
		@Schema(description = "Ingested documents directly in this topic", example = "3") Long ingestedDocuments) {
}
