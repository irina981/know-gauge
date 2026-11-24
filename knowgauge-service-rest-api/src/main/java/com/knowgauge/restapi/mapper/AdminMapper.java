package com.knowgauge.restapi.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.TenantSummaryDto;
import com.knowgauge.core.model.TenantSummary;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdminMapper {

	TenantSummaryDto toDto(TenantSummary workspace);

	List<TenantSummaryDto> toDtos(List<TenantSummary> workspaces);
}
