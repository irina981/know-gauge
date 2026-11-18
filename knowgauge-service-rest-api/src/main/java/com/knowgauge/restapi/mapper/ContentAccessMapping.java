package com.knowgauge.restapi.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.knowgauge.core.service.content.ContentScope;

/**
 * MapStruct helper for content DTOs (ADR010): {@code readOnly} (the caller may not change it) and {@code shared}
 * (it belongs to the library).
 */
@Component
public class ContentAccessMapping {

	private final ContentScope contentScope;

	public ContentAccessMapping(ContentScope contentScope) {
		this.contentScope = contentScope;
	}

	@Named("readOnly")
	public boolean readOnly(Long tenantId) {
		return !contentScope.canWrite(tenantId);
	}

	/** Whether the content belongs to a library tenant, whoever is asking (admins and fixed mode included). */
	@Named("shared")
	public boolean shared(Long tenantId) {
		return contentScope.isLibrary(tenantId);
	}
}
