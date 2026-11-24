package com.knowgauge.contract.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A tenant-level setting of one tenant (ADR014): the system value, the tenant's own value, and the value in effect for it")
public record TenantSettingDto(
		@Schema(description = "The property name, e.g. kg.settings.tenant.monthly-token-limit") String key,
		@Schema(description = "Section of the dialog, as on the System settings page, e.g. AI models") String group,
		String label,
		String description,
		@Schema(description = "INTEGER, TEXT or CHOICE") String type,
		@Schema(description = "Smallest allowed INTEGER") Long min,
		@Schema(description = "Largest allowed INTEGER") Long max,
		@Schema(description = "Allowed values of a CHOICE") List<String> options,
		@Schema(description = "Values to offer for a TEXT setting, e.g. model names from the price catalog; other text is allowed") List<String> suggestions,
		@Schema(description = "CONFIG (a configuration value) or LIMIT, from setting_configs; the dialog shows each on a tab of its own") String settingType,
		@Schema(description = "The system value in effect; null when empty") String systemValue,
		@Schema(description = "The tenant's own value; null when it uses the system value") String ownValue,
		@Schema(description = "The value in effect for the tenant") String value) {
}
