package com.knowgauge.contract.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "A workspace's own setting values (the Limits dialog's Save)")
public record WorkspaceSettingsInput(
		@Schema(description = "Values as text by setting key; an empty value removes the workspace's own value, so the system value applies. For a limit, 0 means no limit") @NotNull Map<String, String> values) {
}
