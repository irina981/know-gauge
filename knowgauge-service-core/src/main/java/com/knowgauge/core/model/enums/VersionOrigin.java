package com.knowgauge.core.model.enums;

/**
 * How a question version came to exist.
 */
public enum VersionOrigin {
	/** Created by the initial test generation (version 1). */
	GENERATED,
	/** Created by a reviewer-requested LLM regeneration. */
	REGENERATED,
	/** Created by accepting the verifier's suggested replacements for flagged options. */
	FIX_ACCEPTED,
	/** Written manually by a reviewer. */
	MANUAL
}
