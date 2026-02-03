package com.knowgauge.core.context;

import java.util.OptionalLong;

public interface ExecutionContext {
	long tenantId(); // always present

	OptionalLong userId(); // may be absent (jobs, anonymous)
}
