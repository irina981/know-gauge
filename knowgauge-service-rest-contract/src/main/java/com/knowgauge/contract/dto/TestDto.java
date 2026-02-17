package com.knowgauge.contract.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TestDto(
		Long id,
		Long tenantId,
		List<Long> topicIds,
		List<Long> documentIds,
		String difficulty,
		boolean avoidRepeats,
		String coverageMode,
		Integer questionCount,
		String status,
		String generationModel,
		String promptTemplateId,
		Map<String, Object> generationParams,
		Instant generationStartedAt,
		Instant generationFinishedAt,
		Instant generationFailedAt,
		String generationErrorMessage) {
}
