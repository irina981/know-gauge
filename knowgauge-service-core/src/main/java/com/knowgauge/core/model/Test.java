package com.knowgauge.core.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.core.model.enums.TestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Test extends AuditableObject {

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

}
