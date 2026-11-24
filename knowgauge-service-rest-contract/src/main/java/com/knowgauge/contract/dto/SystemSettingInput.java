package com.knowgauge.contract.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "New values for system settings (the page's Save)")
public record SystemSettingInput(
		@Schema(description = "Values as text by setting key; an empty value means empty for an optional setting (e.g. no limit)") @NotNull Map<String, String> values) {
}
