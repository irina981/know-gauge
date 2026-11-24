package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A system setting: its properties-file default, the override saved on the System settings page, and the value in effect")
public record SystemSettingDto(
		@Schema(description = "The property name, e.g. kg.testgen.defaults.generation-model") String key,
		@Schema(description = "Section of the page, e.g. Workspace limits") String group,
		String label,
		String description,
		@Schema(description = "INTEGER, TEXT or CHOICE") String type,
		@Schema(description = "Allowed values of a CHOICE") List<String> options,
		@Schema(description = "Smallest allowed INTEGER") Long min,
		@Schema(description = "Largest allowed INTEGER") Long max,
		@Schema(description = "Whether the setting may be empty (e.g. no limit)") boolean optional,
		@Schema(description = "Value from the properties files; null when empty") String defaultValue,
		@Schema(description = "Whether a value saved on the page overrides the default") boolean overridden,
		@Schema(description = "Value saved on the page; null when not overridden or saved empty") String overrideValue,
		@Schema(description = "Value in effect; null when empty") String value,
		@Schema(description = "Values to offer for a TEXT setting, e.g. model names from the price catalog; other text is allowed") List<String> suggestions,
		@Schema(description = "Whether a tenant may have a value of its own, overriding this one (ADR014)") boolean tenantLevel,
		@Schema(description = "CONFIG (a configuration value) or LIMIT (a limit on what a workspace or user may use); the page shows each on a tab of its own") String settingType) {
}
