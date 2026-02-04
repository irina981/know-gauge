package com.knowgauge.restapi.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.multipart.MultipartFile;

import com.knowgauge.contract.dto.DocumentDto;
import com.knowgauge.contract.dto.DocumentInput;
import com.knowgauge.core.model.Document;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentMapper {

	/**
	 * Input DTO -> Domain Note: file stream is passed separately to service, so it
	 * is not mapped here.
	 */
	@Mapping(target = "id", ignore = true) // created by DB
	@Mapping(target = "storageKey", ignore = true) // set by storage layer
	@Mapping(target = "etag", ignore = true) // set after upload
	@Mapping(target = "version", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "ingestedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true) // set by service/DB
	@Mapping(target = "createdBy", ignore = true) // set by service/DB
	@Mapping(target = "updatedAt", ignore = true) // set by service/DB
	@Mapping(target = "updatedBy", ignore = true) // set by service/DB
	@Mapping(target = "errorMessage", ignore = true)
	@Mapping(target = "tenantId", ignore = true)
	@Mapping(target = "checksum", ignore = true)
	@Mapping(target = "originalFileName", expression = "java(input != null && input.originalFileName() != null ? input.originalFileName() : file.getOriginalFilename())")
	@Mapping(target = "contentType", expression = "java(input != null && input.contentType() != null ? input.contentType() : file.getContentType())")
	@Mapping(target = "fileSizeBytes", expression = "java((input != null && input.fileSizeBytes() > 0) ? input.fileSizeBytes() : file.getSize())")
	Document toDomain(DocumentInput input, MultipartFile file);

	/**
	 * Domain -> Response DTO
	 */
	DocumentDto toDto(Document document);
}
