package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.TopicDto;
import com.knowgauge.contract.dto.TopicCreateInput;
import com.knowgauge.contract.dto.TopicTreeNodeInput;
import com.knowgauge.core.model.Topic;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TopicMapper {

    /**
     * Input DTO -> Domain
     * For create requests you usually ignore id and let persistence assign it.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "createdAt", ignore = true)      // set by service/DB
    @Mapping(target = "createdBy", ignore = true)      // set by service/DB
    @Mapping(target = "updatedAt", ignore = true)      // set by service/DB
    @Mapping(target = "updatedBy", ignore = true)      // set by service/DB
    Topic toDomain(TopicCreateInput input);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "parentId", ignore = true) // set later in service while linking tree
    @Mapping(target = "path", ignore = true)
    @Mapping(target = "createdAt", ignore = true)      // set by service/DB
    @Mapping(target = "createdBy", ignore = true)      // set by service/DB
    @Mapping(target = "updatedAt", ignore = true)      // set by service/DB
    @Mapping(target = "updatedBy", ignore = true)      // set by service/DB
    Topic toDomain(TopicTreeNodeInput input);

    /**
     * Domain -> Response DTO
     * If Topic has children, MapStruct will map List<Topic> -> List<TopicDto> automatically.
     */
    TopicDto toDto(Topic topic);
}
