package com.knowgauge.core.service.retrieving;

import java.util.Collection;
import java.util.List;

import com.knowgauge.core.model.ChunkEmbedding;
import com.knowgauge.core.model.enums.TestCoverageMode;

public interface RetrievingService {

	/**
	 * Selects the chunks a test is generated from.
	 *
	 * @param tenantIds    tenants whose content may be used: the test's own tenant and the library
	 * @param coverageMode balanced random sampling, or FOCUSED similarity search
	 * @param focusQuery   text to search for; required for FOCUSED, ignored otherwise
	 */
	public List<ChunkEmbedding> retrieveTop(Collection<Long> tenantIds, Collection<Long> documentIds, int limit,
			TestCoverageMode coverageMode, String focusQuery, boolean avoidRepeats);
}
