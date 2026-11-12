package com.knowgauge.restapi.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.contract.dto.TestQuestionOptionDto;
import com.knowgauge.contract.dto.TestQuestionVersionDto;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.TestQuestionOption;
import com.knowgauge.core.model.TestQuestionVersion;
import com.knowgauge.core.model.enums.OptionLetter;

/**
 * Maps question slots and versions to API DTOs.
 */
@Component
public class TestQuestionMapper {

	public TestQuestionDto toDto(TestQuestion question) {
		if (question == null) {
			return null;
		}
		TestQuestionVersion v = question.getCurrentVersion();
		return new TestQuestionDto(question.getId(), question.getTestId(), question.getQuestionIndex(),
				question.getStatus() != null ? question.getStatus().name() : null, question.getCurrentVersionId(),
				v != null ? v.getVersionNo() : null, v != null && v.getOrigin() != null ? v.getOrigin().name() : null,
				question.getQuestionText(), mapOptions(question.getOptions()), names(question.getCorrectOptions()),
				names(question.getFlaggedOptions()), question.getExplanation(), question.getSourceChunkIds());
	}

	public TestQuestionVersionDto toDto(TestQuestionVersion v) {
		if (v == null) {
			return null;
		}
		return new TestQuestionVersionDto(v.getId(), v.getQuestionId(), v.getVersionNo(),
				v.getOrigin() != null ? v.getOrigin().name() : null, v.getParentVersionId(), v.getGenerationRunId(),
				v.getVerificationRunId(), v.getQuestionText(), mapOptions(v.getOptions()), names(v.getCorrectOptions()),
				names(v.getFlaggedOptions()), v.getExplanation(), v.getSourceChunkIds(), v.getCreatedAt(),
				v.getCreatedBy());
	}

	private Map<String, TestQuestionOptionDto> mapOptions(Map<OptionLetter, TestQuestionOption> options) {
		Map<String, TestQuestionOptionDto> result = new LinkedHashMap<>();
		if (options == null) {
			return result;
		}
		for (OptionLetter letter : OptionLetter.values()) {
			TestQuestionOption o = options.get(letter);
			if (o != null) {
				result.put(letter.name(), new TestQuestionOptionDto(letter.name(), o.getText(), o.isCorrect(),
						o.isFlagged(), o.getSuggestedText(), o.getFlagReason()));
			}
		}
		return result;
	}

	private List<String> names(List<OptionLetter> letters) {
		return letters == null ? List.of() : letters.stream().map(Enum::name).toList();
	}
}
