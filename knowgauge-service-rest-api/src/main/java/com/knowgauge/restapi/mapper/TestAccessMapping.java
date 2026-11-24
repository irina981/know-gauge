package com.knowgauge.restapi.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.knowgauge.core.context.TenantViewProvider;
import com.knowgauge.core.model.Test;
import com.knowgauge.core.service.access.RolePermissions;

/**
 * MapStruct helper for {@code TestDto.readOnly}: whether the caller may change the test (ADR017), so the UI hides
 * what the server would refuse. An ADMIN viewing another workspace changes nothing (ADR013).
 */
@Component
public class TestAccessMapping {

	private final RolePermissions permissions;
	private final TenantViewProvider tenantView;

	public TestAccessMapping(RolePermissions permissions, TenantViewProvider tenantView) {
		this.permissions = permissions;
		this.tenantView = tenantView;
	}

	@Named("testReadOnly")
	public boolean readOnly(Test test) {
		return tenantView.isViewingOtherTenant() || !permissions.canChangeTest(test);
	}
}
