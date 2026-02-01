package com.knowgauge.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class PersistedObject {

	protected Long id;

	// ---- optional helpers ----

	public boolean isNew() {
		return id == null;
	}
}
