package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.TestQuestionDto;
import com.knowgauge.core.model.TestQuestion;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TestQuestionMapper {

	TestQuestionDto toDto(TestQuestion question);
}
