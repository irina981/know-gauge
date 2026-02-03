package com.knowgauge.core.context;

import java.util.OptionalLong;

public interface CurrentUserProvider {
	OptionalLong userId();
}
