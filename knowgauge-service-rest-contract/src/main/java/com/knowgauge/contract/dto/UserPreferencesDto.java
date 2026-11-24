package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Settings the user chose for the app. A null field was never chosen: the app uses its default.")
public record UserPreferencesDto(
		@Schema(description = "Colour scheme; SYSTEM follows the device", example = "SYSTEM", allowableValues = {
				"LIGHT", "DARK", "SYSTEM" }) String theme) {
}
