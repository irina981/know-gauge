package com.knowgauge.restapi.mapper;


import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.web.multipart.MultipartFile;

import com.knowgauge.contract.dto.DocumentDto;
import com.knowgauge.contract.dto.DocumentInput;
import com.knowgauge.core.model.Document;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface DocumentMapper {

    /**
     * Input DTO -> Domain
     * Note: file stream is passed separately to service, so it is not mapped here.
     */
    @Mapping(target = "id", ignore = true)              // created by DB
    @Mapping(target = "storageKey", ignore = true)       // set by storage layer
    @Mapping(target = "etag", ignore = true)            // set after upload
    @Mapping(target = "version", ignore = true) 
    @Mapping(target = "status", ignore = true) 
    @Mapping(target = "ingestedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)      // set by service/DB
    @Mapping(target = "createdBy", ignore = true)      // set by service/DB
    @Mapping(target = "updatedAt", ignore = true)      // set by service/DB
    @Mapping(target = "updatedBy", ignore = true)      // set by service/DB
    @Mapping(target = "errorMessage", ignore = true) 
    Document toDomain(DocumentInput input, @Context MultipartFile file);

    /**
     * Domain -> Response DTO
     */
    DocumentDto toDto(Document document);
    
    @AfterMapping
    default void fillFromFileIfMissing(
            @MappingTarget Document document,
            DocumentInput input,
            @Context MultipartFile file
    ) {
        if (document.getOriginalFileName() == null) {
            document.setOriginalFileName(file.getOriginalFilename());
        }
        if (document.getContentType() == null) {
            document.setContentType(file.getContentType());
        }
        // fileSizeBytes is usually primitive long; if so, check for 0 instead of null
        if (document.getFileSizeBytes() == 0L) {
            document.setFileSizeBytes(file.getSize());
        }
    }
}

