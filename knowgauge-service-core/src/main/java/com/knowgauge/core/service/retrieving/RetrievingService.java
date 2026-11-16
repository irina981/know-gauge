package com.knowgauge.core.service.retrieving;

import java.util.Collection;
import java.util.List;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.enums.TestCoverageMode;

public interface RetrievingService {

	/**
	 * Selects the chunks a test is generated from.
	 *
	 * @param coverageMode balanced random sampling, or FOCUSED similarity search
	 * @param focusQuery   text to search for; required for FOCUSED, ignored otherwise
	 */
	public List<ChunkEmbedding> retrieveTop(Long tenantId, Collection<Long> documentIds, int limit,
			TestCoverageMode coverageMode, String focusQuery, boolean avoidRepeats);
}
