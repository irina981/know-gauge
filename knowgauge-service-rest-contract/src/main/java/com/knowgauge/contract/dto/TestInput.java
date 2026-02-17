package com.knowgauge.contract.dto;

import java.util.List;
import java.util.Map;

public record TestInput(List<Long> topicIds, List<Long> documentIds, String difficulty, boolean avoidRepeats,
		String coverageMode, Integer questionCount, String language, String generationModel, String promptTemplateId, Map<String, Object> generationParams) {
}
