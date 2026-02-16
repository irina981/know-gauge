package com.knowgauge.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	private Long tenantId;

	@Builder.Default
	private List<Long> topicIds = new ArrayList<Long>();
	
	@Builder.Default
	private List<Long> documentIds = new ArrayList<Long>();

	@Builder.Default
	private List<DocumentChunk> usedChunks = new ArrayList<DocumentChunk>();

	private TestDifficulty difficulty;

	@Builder.Default
	private boolean avoidRepeats = true;

	@Builder.Default
	private TestCoverageMode coverageMode = TestCoverageMode.BALANCED_PER_DOC_CHUNKS;

	private Integer questionCount;

	private TestStatus status;

	private String generationModel;
	
	private String promptTemplateId;

	private Map<String, Object> generationParams;

}
