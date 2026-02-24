package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.TestDto;
import com.knowgauge.contract.dto.TestInput;
import com.knowgauge.core.model.Test;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = { DocumentChunkMapper.class,
		EnumMappingHelper.class })
public interface TestMapper {

	/**
	 * Input DTO -> Domain For create requests you usually ignore id and let
	 * persistence assign it.
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "tenantId", ignore = true)
	@Mapping(target = "usedChunks", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "generationStartedAt", ignore = true)
	@Mapping(target = "generationFinishedAt", ignore = true)
	@Mapping(target = "generationFailedAt", ignore = true)
	@Mapping(target = "generationErrorMessage", ignore = true)
	@Mapping(target = "minMultipleCorrectQuestionsCount", ignore = true)
	@Mapping(target = "createdAt", ignore = true) // set by service/DB
	@Mapping(target = "createdBy", ignore = true) // set by service/DB
	@Mapping(target = "updatedAt", ignore = true) // set by service/DB
	@Mapping(target = "updatedBy", ignore = true) // set by service/DB
	Test toDomain(TestInput input);

	/**
	 * Domain -> Response DTO If Topic has children, MapStruct will map List<Topic>
	 * -> List<TopicDto> automatically.
	 */
	TestDto toDto(Test test);
}
