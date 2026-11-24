package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.UsageSummaryDto;
import com.knowgauge.contract.dto.UserPreferencesDto;
import com.knowgauge.core.model.UsageSummary;
import com.knowgauge.core.model.UserPreferences;
import com.knowgauge.core.model.enums.ThemePreference;
import com.knowgauge.restapi.util.EnumParser;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AccountMapper {

	UserPreferencesDto toDto(UserPreferences preferences);

	UserPreferences toDomain(UserPreferencesDto dto);

	UsageSummaryDto toDto(UsageSummary usage);

	default ThemePreference toTheme(String value) {
		return EnumParser.parse(ThemePreference.class, value);
	}

	default String toString(ThemePreference value) {
		return value != null ? value.name() : null;
	}
}
