package com.knowgauge.core.service.retrieving;

import java.util.Collection;
import java.util.List;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.enums.TestCoverageMode;

public interface RetrievingService {
	public List<ChunkEmbedding> retrieveTop(Long tenantId, Collection<Long> documentIds, int limit,
			TestCoverageMode coverageMode, boolean avoidRepeats);
}
