package com.knowgauge.contract.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A workspace-level setting of one workspace (ADR014): the system value, the workspace's own value, and the value in effect for it")
public record WorkspaceSettingDto(
		@Schema(description = "The property name, e.g. kg.settings.tenant.monthly-token-limit") String key,
		String label,
		String description,
		@Schema(description = "INTEGER, TEXT or CHOICE") String type,
		@Schema(description = "Smallest allowed INTEGER") Long min,
		@Schema(description = "Largest allowed INTEGER") Long max,
		@Schema(description = "The system value in effect; null when empty") String systemValue,
		@Schema(description = "The workspace's own value; null when it uses the system value") String ownValue,
		@Schema(description = "The value in effect for the workspace") String value) {
}
