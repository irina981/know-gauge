package com.knowgauge.core.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.model.enums.TestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Test {

	private Long id;

	private Long topicId;

	@Builder.Default
	private Set<DocumentChunk> usedChunks = new HashSet<>();

	private TestDifficulty difficulty;

	@Builder.Default
	private boolean avoidRepeats = true;

	@Builder.Default
	private TestCoverageMode coverageMode = TestCoverageMode.BALANCED;

	private Integer questionCount;

	private TestStatus status;

	private String generationModel;

	private Map<String, Object> generationParams;

	private Instant createdAt;

}
