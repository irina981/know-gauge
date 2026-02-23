package com.knowgauge.restapi.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.core.model.TestQuestion;
import com.knowgauge.core.model.enums.AnswerOption;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TestQuestionMapper {

	TestQuestionDto toDto(TestQuestion question);

	default List<String> mapAnswerOptions(List<AnswerOption> options) {
		if (options == null) {
			return null;
		}
		return options.stream().map(Enum::name).toList();
	}
}
