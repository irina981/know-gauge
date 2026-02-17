package com.knowgauge.contract.dto;

public record DocumentChunkDto(Long id, Long tenantId, Long topicId, Long documentId, Integer documentVersion,
		Long sectionId, Integer ordinal, String chunkText, Integer startPage, Integer endPage, Integer charStart,
		Integer charEnd, String checksum) {

}
