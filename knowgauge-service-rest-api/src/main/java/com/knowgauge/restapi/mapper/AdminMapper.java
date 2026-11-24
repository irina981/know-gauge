package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.knowgauge.contract.dto.TenantSummaryDto;
import com.knowgauge.contract.dto.TokenPriceDto;
import com.knowgauge.contract.dto.TokenPriceSuggestionDto;
import com.knowgauge.core.model.TenantSummary;
import com.knowgauge.core.model.TokenPrice;
import com.knowgauge.core.model.TokenPriceSuggestion;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdminMapper {

	TenantSummaryDto toDto(TenantSummary workspace);

	TokenPriceDto toDto(TokenPrice price);

	TokenPriceSuggestionDto toDto(TokenPriceSuggestion suggestion);
}
