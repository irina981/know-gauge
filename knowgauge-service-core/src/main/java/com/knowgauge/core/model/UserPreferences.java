package com.knowgauge.core.model;

import java.io.Serializable;

import com.knowgauge.core.model.enums.ThemePreference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Settings a user chose for the app, kept on their user row so they follow them to every device. A field that is
 * {@code null} was never chosen: the app uses its own default.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences implements Serializable {

	// Stored as a JSON column: Hypersistence copies the value through Java serialization.
	private static final long serialVersionUID = 1L;

	private ThemePreference theme;

	public static UserPreferences defaults() {
		return new UserPreferences();
	}
}
