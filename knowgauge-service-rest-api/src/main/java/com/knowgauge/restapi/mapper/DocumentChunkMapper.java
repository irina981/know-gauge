package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.DocumentChunkDto;
import com.knowgauge.core.model.DocumentChunk;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface DocumentChunkMapper {

    /**
     * Domain -> Response DTO
     * If Topic has children, MapStruct will map List<Topic> -> List<TopicDto> automatically.
     */
    DocumentChunkDto toDto(DocumentChunk chunk);
}
