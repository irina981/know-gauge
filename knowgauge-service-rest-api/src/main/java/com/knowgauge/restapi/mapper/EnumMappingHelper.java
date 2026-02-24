package com.knowgauge.restapi.mapper;

import org.springframework.stereotype.Component;

import com.knowgauge.core.model.enums.AnswerCardinality;
import com.knowgauge.core.model.enums.Language;
import com.knowgauge.core.model.enums.TestCoverageMode;
import com.knowgauge.core.model.enums.TestDifficulty;
import com.knowgauge.restapi.util.EnumParser;

/**
 * MapStruct helper for converting string inputs to enum values.
 * Used by mappers to handle case-insensitive enum parsing from API inputs.
 */
@Component
public class EnumMappingHelper {

	public TestDifficulty toTestDifficulty(String value) {
		return EnumParser.parse(TestDifficulty.class, value);
	}

	public TestCoverageMode toTestCoverageMode(String value) {
		return EnumParser.parse(TestCoverageMode.class, value);
	}

	public AnswerCardinality toAnswerCardinality(String value) {
		return EnumParser.parse(AnswerCardinality.class, value);
	}

	public Language toLanguage(String value) {
		return EnumParser.parse(Language.class, value);
	}

	// Reverse mappings for DTOs
	public String toString(TestDifficulty value) {
		return value != null ? value.name() : null;
	}

	public String toString(TestCoverageMode value) {
		return value != null ? value.name() : null;
	}

	public String toString(AnswerCardinality value) {
		return value != null ? value.name() : null;
	}

	public String toString(Language value) {
		return value != null ? value.name() : null;
	}
}
